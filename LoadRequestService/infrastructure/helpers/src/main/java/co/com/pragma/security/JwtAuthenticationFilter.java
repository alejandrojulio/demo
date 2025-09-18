package co.com.pragma.security;

import co.com.pragma.security.client.AuthServiceClient;
import co.com.pragma.security.dto.TokenValidationResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements WebFilter {

    private final AuthServiceClient authServiceClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TOKEN_HEADER = "token";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().toString();

        log.info("🔒 JwtAuthenticationFilter EJECUTÁNDOSE - Procesando solicitud: {} {}", method, path);
        System.out.println("🔒 JwtAuthenticationFilter EJECUTÁNDOSE - Procesando solicitud: " + method + " " + path);

        if (isPublicPath(path)) {
            log.info("Ruta pública, permitiendo acceso sin autenticación: {}", path);
            return chain.filter(exchange);
        }
        
        // Verificar si es una llamada de servicio interno
        if (isInternalServiceCall(request)) {
            log.info("Llamada de servicio interno autenticada: {} {}", method, path);
            return chain.filter(exchange);
        }

        String token = extractTokenFromRequest(request);
        if (token == null) {
            log.warn("Token JWT no encontrado en la solicitud a: {} {} - Headers: {}", method, path, request.getHeaders().toSingleValueMap());
            return handleUnauthorized(exchange, "Token de autorización requerido");
        }
        
        log.debug("Token extraído exitosamente para: {} {}", method, path);

        // Validar token con el microservicio de autenticación
        return authServiceClient.validateToken(token)
                .flatMap(tokenResponse -> {
                    if (!tokenResponse.isValid()) {
                        log.warn("Token JWT inválido para solicitud: {} {}", method, path);
                        return handleUnauthorized(exchange, "Token inválido o expirado");
                    }

                    // Validar permisos específicos para cada endpoint
                    if (!hasPermission(path, method, tokenResponse)) {
                        log.warn("Usuario {} sin permisos para: {} {} (rol: {})", 
                               tokenResponse.getEmail(), method, path, tokenResponse.getRole());
                        return handleForbidden(exchange, "No tiene permisos para acceder a este recurso");
                    }

                    // Agregar información del usuario a los headers para uso downstream
                    String userRole = tokenResponse.getRole() != null ? String.valueOf(tokenResponse.getRole()) : "UNKNOWN";
                    ServerHttpRequest mutatedRequest = request.mutate()
                            .header("X-User-Id", tokenResponse.getId())
                            .header("X-User-Email", tokenResponse.getEmail())
                            .header("X-User-Role", userRole)
                            .header("X-User-Document", tokenResponse.getDocument())
                            .build();

                    log.info("Acceso autorizado para usuario {} (rol: {}) a: {} {}", 
                           tokenResponse.getEmail(), tokenResponse.getRole(), method, path);

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                .onErrorResume(error -> {
                    log.error("Error validando token para solicitud: {} {} - Error: {}", 
                            method, path, error.getMessage());
                    return handleUnauthorized(exchange, "Error de validación de token");
                });
    }

    private String extractTokenFromRequest(ServerHttpRequest request) {
        // Primero intentar obtener token del header "token"
        String token = request.getHeaders().getFirst(TOKEN_HEADER);
        if (token != null && !token.trim().isEmpty()) {
            return token;
        }

        // Si no existe, intentar con Authorization Bearer (por compatibilidad)
        String authHeader = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }

    private boolean isPublicPath(String path) {
        // Paths que no requieren autenticación
        return path.matches("/actuator/.*") || 
               path.matches("/health") ||
               path.matches("/docs/.*") ||
               path.matches("/swagger-ui/.*");
    }
    
    private boolean isInternalServiceCall(ServerHttpRequest request) {
        // Verificar si es una llamada de servicio interno
        String internalHeader = request.getHeaders().getFirst("X-Internal-Service");
        String authHeader = request.getHeaders().getFirst("Authorization");
        
        if (internalHeader != null && authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String expectedToken = System.getProperty("INTERNAL_SERVICE_TOKEN", "CrediYaInternalService2025SecureToken");
            return expectedToken.equals(token);
        }
        
        return false;
    }

    private boolean hasPermission(String path, String method, TokenValidationResponseDTO tokenResponse) {
        String role = tokenResponse.getRole() != null ? String.valueOf(tokenResponse.getRole()) : "UNKNOWN";
        String userId = tokenResponse.getId();

        // Reglas de autorización específicas para SOLICITUDES
        if (path.matches("/api/v1/solicitud.*")) {
            switch (method) {
                case "POST": // Creación de solicitud
                    if (!"CLIENT".equals(role)) {
                        return false;
                    }
                    // Validación adicional: el cliente solo puede crear solicitudes para sí mismo
                    // Esta validación se debe hacer también en el controlador comparando con el body
                    return true;

                case "PUT": // Actualización de solicitud
                    return "ADVISOR".equals(role);

                case "GET": // Listado de solicitudes
                    return "ADVISOR".equals(role);

                default:
                    return false;
            }
        }

        // Por defecto, denegar acceso
        return false;
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, message);
    }

    private Mono<Void> handleForbidden(ServerWebExchange exchange, String message) {
        return writeErrorResponse(exchange, HttpStatus.FORBIDDEN, message);
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", message,
                "status", status.value(),
                "timestamp", System.currentTimeMillis()
        );

        try {
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = response.bufferFactory()
                    .wrap(jsonResponse.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("Error escribiendo respuesta de error", e);
            DataBuffer buffer = response.bufferFactory()
                    .wrap("{\"success\":false,\"message\":\"Error interno del servidor\"}"
                            .getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        }
    }
}
