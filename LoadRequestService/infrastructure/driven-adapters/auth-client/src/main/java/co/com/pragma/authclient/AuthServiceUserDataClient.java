package co.com.pragma.authclient;

import co.com.pragma.model.user.UserData;
import co.com.pragma.model.user.gateways.UserDataGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

/**
 * Cliente HTTP para comunicarse con AuthService
 * Implementa el puerto UserDataGateway del dominio
 */
@Component("userDataAuthClient")
@Slf4j
public class AuthServiceUserDataClient implements UserDataGateway {

    private final WebClient authServiceWebClient;
    
    @Value("${app.internal.service-token:CrediYaInternalService2025SecureToken}")
    private String internalServiceToken;
    
    public AuthServiceUserDataClient(WebClient authServiceWebClient) {
        this.authServiceWebClient = authServiceWebClient;
    }

    @Override
    public Mono<UserData> getUserByDocument(String document) {
        log.info("Consultando datos de usuario por documento: {} desde AuthService", document);
        
        return authServiceWebClient
                .get()
                .uri("/api/v1/users/document/{document}", document)
                .header("X-Service-Auth", internalServiceToken)
                .header("X-Service-Name", "LoadRequestService")
                .retrieve()
                .bodyToMono(AuthServiceApiResponse.class)
                .retryWhen(Retry.fixedDelay(2, Duration.ofMillis(500)))
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        log.info("Usuario encontrado: {} {}", 
                                response.getData().getFirstName(), 
                                response.getData().getLastName());
                        return mapToUserData(response.getData());
                    } else {
                        log.warn("Usuario no encontrado o respuesta inválida para documento: {}", document);
                        throw new UserNotFoundException("Usuario no encontrado: " + document);
                    }
                })
                .onErrorResume(Exception.class, error -> {
                    log.error("Error consultando usuario por documento {} desde AuthService: {}", 
                              document, error.getMessage(), error);
                    return Mono.error(new AuthServiceCommunicationException(
                            "Error comunicándose con AuthService: " + error.getMessage(), error));
                });
    }

    @Override
    public Mono<UserData> getUserByEmail(String email) {
        log.info("Consultando datos de usuario por email: {} desde AuthService", email);
        
        return authServiceWebClient
                .get()
                .uri("/api/v1/users/email/{email}", email)
                .header("X-Service-Auth", internalServiceToken)
                .header("X-Service-Name", "LoadRequestService")
                .retrieve()
                .bodyToMono(AuthServiceApiResponse.class)
                .retryWhen(Retry.fixedDelay(2, Duration.ofMillis(500)))
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        log.info("Usuario encontrado por email: {} {}", 
                                response.getData().getFirstName(), 
                                response.getData().getLastName());
                        return mapToUserData(response.getData());
                    } else {
                        log.warn("Usuario no encontrado o respuesta inválida para email: {}", email);
                        throw new UserNotFoundException("Usuario no encontrado: " + email);
                    }
                })
                .onErrorResume(Exception.class, error -> {
                    log.error("Error consultando usuario por email {} desde AuthService: {}", 
                              email, error.getMessage(), error);
                    return Mono.error(new AuthServiceCommunicationException(
                            "Error comunicándose con AuthService: " + error.getMessage(), error));
                });
    }

    @Override
    public Mono<Boolean> validateUserExists(String document) {
        log.debug("Validando existencia de usuario: {} en AuthService", document);
        
        return authServiceWebClient
                .get()
                .uri("/api/v1/users/validate/{document}", document)
                .retrieve()
                .bodyToMono(AuthServiceValidationResponse.class)
                .retryWhen(Retry.fixedDelay(2, Duration.ofMillis(500)))
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        Map<String, Object> validationData = response.getData();
                        boolean exists = (Boolean) validationData.getOrDefault("exists", false);
                        boolean isActive = (Boolean) validationData.getOrDefault("isActive", false);
                        
                        log.debug("Validación de usuario {}: exists={}, isActive={}", 
                                 document, exists, isActive);
                        
                        return exists && isActive;
                    } else {
                        log.warn("Respuesta inválida de validación para documento: {}", document);
                        return false;
                    }
                })
                .onErrorResume(Exception.class, error -> {
                    log.error("Error validando usuario {} en AuthService: {}", 
                              document, error.getMessage(), error);
                    return Mono.just(false); // Si hay error, asumir que no existe
                });
    }

    /**
     * Mapea la respuesta del AuthService al modelo de dominio
     */
    private UserData mapToUserData(AuthServiceUserData authUserData) {
        return UserData.builder()
                .id(authUserData.getId())
                .document(authUserData.getDocument())
                .email(authUserData.getEmail())
                .firstName(authUserData.getFirstName())
                .lastName(authUserData.getLastName())
                .phone(authUserData.getPhone())
                .baseSalary(authUserData.getBaseSalary())
                .isActive(authUserData.getIsActive())
                .emailVerified(authUserData.getEmailVerified())
                .createdAt(authUserData.getCreatedAt())
                .build();
    }

    /**
     * DTO para respuesta de API del AuthService
     */
    public static class AuthServiceApiResponse {
        private boolean success;
        private String message;
        private AuthServiceUserData data;

        // Constructores
        public AuthServiceApiResponse() {}

        // Getters y Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public AuthServiceUserData getData() { return data; }
        public void setData(AuthServiceUserData data) { this.data = data; }
    }

    /**
     * DTO para respuesta de validación del AuthService
     */
    public static class AuthServiceValidationResponse {
        private boolean success;
        private String message;
        private Map<String, Object> data;

        // Constructores
        public AuthServiceValidationResponse() {}

        // Getters y Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
    }

    /**
     * DTO para datos de usuario del AuthService
     */
    public static class AuthServiceUserData {
        private Long id;
        private String document;
        private String email;
        private String firstName;
        private String lastName;
        private String phone;
        private java.math.BigDecimal baseSalary;
        private Boolean isActive;
        private Boolean emailVerified;
        private java.time.LocalDateTime createdAt;

        // Constructor vacío
        public AuthServiceUserData() {}

        // Getters y Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getDocument() { return document; }
        public void setDocument(String document) { this.document = document; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public java.math.BigDecimal getBaseSalary() { return baseSalary; }
        public void setBaseSalary(java.math.BigDecimal baseSalary) { this.baseSalary = baseSalary; }

        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }

        public Boolean getEmailVerified() { return emailVerified; }
        public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }

        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    /**
     * Excepción cuando no se encuentra el usuario
     */
    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Excepción de comunicación con AuthService
     */
    public static class AuthServiceCommunicationException extends RuntimeException {
        public AuthServiceCommunicationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
