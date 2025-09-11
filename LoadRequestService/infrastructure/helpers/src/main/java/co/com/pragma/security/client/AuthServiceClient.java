package co.com.pragma.security.client;

import co.com.pragma.security.dto.TokenValidationRequestDTO;
import co.com.pragma.security.dto.TokenValidationResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${auth-service.base-url:http://localhost:8080}")
    private String authServiceBaseUrl;

    @Value("${auth-service.timeout:5000}")
    private int timeoutMs;

    /**
     * Valida un token JWT llamando al microservicio de autenticación
     */
    public Mono<TokenValidationResponseDTO> validateToken(String token) {
        log.info("Validando token con el servicio de autenticación en URL: {}", authServiceBaseUrl);

        TokenValidationRequestDTO request = TokenValidationRequestDTO.builder()
                .token(token)
                .build();

        return webClientBuilder.build()
                .post()
                .uri(authServiceBaseUrl + "/api/v1/auth/validate-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AuthServiceResponse.class)
                .doOnNext(response -> {
                    log.debug("Respuesta completa del AuthService: success={}, message={}, data={}", 
                            response.isSuccess(), response.getMessage(), response.getData());
                })
                .map(response -> response.getData())
                .timeout(Duration.ofMillis(timeoutMs))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(100)))
                .doOnSuccess(response -> {
                    if (response != null && response.isValid()) {
                        log.info("Token validado exitosamente para usuario: {} con rol: {}", 
                                response.getEmail(), response.getRole());
                    } else {
                        log.warn("Token inválido recibido del servicio de autenticación: {}", 
                                response != null ? response.getError() : "respuesta null");
                    }
                })
                .onErrorResume(error -> {
                    log.error("Error llamando al servicio de autenticación: {} - Tipo: {}", 
                            error.getMessage(), error.getClass().getSimpleName());
                    return Mono.just(TokenValidationResponseDTO.builder()
                            .valid(false)
                            .error("Error de comunicación con el servicio de autenticación")
                            .message("No se pudo validar el token")
                            .build());
                });
    }

    /**
     * Clase interna para mapear la respuesta del API de autenticación
     */
    public static class AuthServiceResponse {
        private boolean success;
        private String message;
        private TokenValidationResponseDTO data;

        // Getters y setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public TokenValidationResponseDTO getData() { return data; }
        public void setData(TokenValidationResponseDTO data) { this.data = data; }
    }
}
