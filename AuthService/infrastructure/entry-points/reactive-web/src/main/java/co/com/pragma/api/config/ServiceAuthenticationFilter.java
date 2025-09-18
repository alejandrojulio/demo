package co.com.pragma.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Filtro de autenticación para comunicación entre microservicios
 * Valida un token interno específico para endpoints de datos de usuario
 */
@Component
@Slf4j
public class ServiceAuthenticationFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    private static final String SERVICE_AUTH_HEADER = "X-Service-Auth";
    private static final String SERVICE_NAME_HEADER = "X-Service-Name";
    
    @Value("${app.internal.service-token:CrediYaInternalService2025SecureToken}")
    private String internalServiceToken;

    @Override
    public Mono<ServerResponse> filter(ServerRequest request, HandlerFunction<ServerResponse> next) {
        String authToken = request.headers().firstHeader(SERVICE_AUTH_HEADER);
        String serviceName = request.headers().firstHeader(SERVICE_NAME_HEADER);
        
        if (authToken == null || serviceName == null) {
            log.warn("Intento de acceso a endpoint interno sin headers de autenticación desde IP: {}", 
                    getClientIP(request));
            return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                    .bodyValue(createErrorResponse("Acceso no autorizado: Se requiere autenticación de servicio"));
        }
        
        if (!internalServiceToken.equals(authToken)) {
            log.warn("Token de servicio inválido desde IP: {} - Servicio reclamado: {}", 
                    getClientIP(request), serviceName);
            return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                    .bodyValue(createErrorResponse("Token de servicio inválido"));
        }
        
        // Validar que sea un servicio autorizado
        if (!isAuthorizedService(serviceName)) {
            log.warn("Servicio no autorizado: {} desde IP: {}", serviceName, getClientIP(request));
            return ServerResponse.status(HttpStatus.FORBIDDEN)
                    .bodyValue(createErrorResponse("Servicio no autorizado"));
        }
        
        log.debug("Acceso autorizado para servicio: {} desde IP: {}", serviceName, getClientIP(request));
        return next.handle(request);
    }
    
    private boolean isAuthorizedService(String serviceName) {
        // Lista de servicios autorizados para acceder a datos de usuario
        return "LoadRequestService".equals(serviceName) || 
               "DebtCapacityService".equals(serviceName) ||
               "NotificationService".equals(serviceName);
    }
    
    private String getClientIP(ServerRequest request) {
        return request.headers().firstHeader("X-Forwarded-For") != null 
            ? request.headers().firstHeader("X-Forwarded-For")
            : request.remoteAddress().map(addr -> addr.getAddress().getHostAddress()).orElse("unknown");
    }
    
    private ErrorResponse createErrorResponse(String message) {
        return new ErrorResponse(false, message, null);
    }
    
    /**
     * Clase interna para respuestas de error
     */
    public static class ErrorResponse {
        private final boolean success;
        private final String message;
        private final Object data;
        
        public ErrorResponse(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Object getData() { return data; }
    }
}
