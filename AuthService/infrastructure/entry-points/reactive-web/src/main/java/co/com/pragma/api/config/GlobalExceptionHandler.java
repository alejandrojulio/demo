package co.com.pragma.api.config;

import co.com.pragma.api.dto.ApiResponse;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

@Component
@Order(-2)
@Slf4j
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status;
        String message;
        String details = null;

        // Manejo específico por tipo de excepción
        if (ex instanceof JwtException) {
            status = HttpStatus.UNAUTHORIZED;
            message = "Error de autenticación";
            details = ex.getMessage();
            log.warn("JWT Exception: {}", ex.getMessage());
        } else if (ex instanceof IllegalArgumentException) {
            status = HttpStatus.BAD_REQUEST;
            message = "Datos de entrada inválidos";
            details = ex.getMessage();
            log.warn("Validation Error: {}", ex.getMessage());
        } else if (ex instanceof RuntimeException) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "Error interno del servidor";
            details = "Ha ocurrido un error inesperado";
            log.error("Runtime Exception: ", ex);
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "Error interno del servidor";
            details = "Ha ocurrido un error inesperado";
            log.error("Unexpected Exception: ", ex);
        }

        // Configurar la respuesta
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        // Crear respuesta de error
        ApiResponse<String> errorResponse = ApiResponse.<String>builder()
                .success(false)
                .message(message)
                .data(details)
                .build();

        try {
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory()
                    .wrap(jsonResponse.getBytes(StandardCharsets.UTF_8));
            
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("Error creating error response", e);
            DataBuffer buffer = exchange.getResponse().bufferFactory()
                    .wrap("{\"success\":false,\"message\":\"Error interno del servidor\",\"data\":null}"
                            .getBytes(StandardCharsets.UTF_8));
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }
    }
}
