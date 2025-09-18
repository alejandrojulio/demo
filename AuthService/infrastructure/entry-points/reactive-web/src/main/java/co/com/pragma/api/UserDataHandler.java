package co.com.pragma.api;

import co.com.pragma.api.dto.ApiResponse;
import co.com.pragma.model.user.gateways.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Handler para endpoints de consulta de datos de usuario (para otros microservicios)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserDataHandler {

    private final UserRepository userRepository;

    /**
     * Obtiene datos básicos de usuario por documento
     * Endpoint para comunicación entre microservicios
     */
    public Mono<ServerResponse> getUserByDocument(ServerRequest serverRequest) {
        String document = serverRequest.pathVariable("document");
        
        log.info("Consultando datos de usuario por documento: {}", document);
        
        return userRepository.findByDocument(document)
                .flatMap(user -> {
                    if (!user.isActive()) {
                        log.warn("Usuario con documento {} está inactivo", document);
                        return ServerResponse.status(HttpStatus.NOT_FOUND)
                                .bodyValue(createErrorResponse("Usuario no encontrado o inactivo"));
                    }
                    
                    // Crear respuesta con datos necesarios para préstamos (sin información sensible)
                    UserDataResponse userData = UserDataResponse.builder()
                            .id(Long.valueOf(user.getId()))
                            .document(user.getDocument())
                            .email(user.getEmail())
                            .firstName(user.getFirstName())
                            .lastName(user.getLastName())
                            .phone(user.getPhone())
                            .baseSalary(user.getBaseSalary())
                            .isActive(user.isActive())
                            .emailVerified(true) // Por defecto true, el AuthService no maneja verificación
                            .createdAt(user.getCreatedAt())
                            .build();
                    
                    log.info("Datos de usuario encontrados para documento: {} - {} {}", 
                             document, user.getFirstName(), user.getLastName());
                    
                    ApiResponse<UserDataResponse> response = ApiResponse.success(
                            userData,
                            "Consulta de usuario exitosa"
                    );
                    
                    return ServerResponse.ok().bodyValue(response);
                })
                .switchIfEmpty(
                    ServerResponse.status(HttpStatus.NOT_FOUND)
                            .bodyValue(createErrorResponse("Usuario no encontrado"))
                )
                .onErrorResume(Exception.class, error -> {
                    log.error("Error consultando usuario por documento {}: {}", document, error.getMessage(), error);
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .bodyValue(createErrorResponse("Error interno del servidor"));
                });
    }

    /**
     * Obtiene datos básicos de usuario por email
     * Endpoint para comunicación entre microservicios
     */
    public Mono<ServerResponse> getUserByEmail(ServerRequest serverRequest) {
        String email = serverRequest.pathVariable("email");
        
        log.info("Consultando datos de usuario por email: {}", email);
        
        return userRepository.findByEmail(email)
                .flatMap(user -> {
                    if (!user.isActive()) {
                        log.warn("Usuario con email {} está inactivo", email);
                        return ServerResponse.status(HttpStatus.NOT_FOUND)
                                .bodyValue(createErrorResponse("Usuario no encontrado o inactivo"));
                    }
                    
                    UserDataResponse userData = UserDataResponse.builder()
                            .id(Long.valueOf(user.getId()))
                            .document(user.getDocument())
                            .email(user.getEmail())
                            .firstName(user.getFirstName())
                            .lastName(user.getLastName())
                            .phone(user.getPhone())
                            .baseSalary(user.getBaseSalary())
                            .isActive(user.isActive())
                            .emailVerified(true) // Por defecto true
                            .createdAt(user.getCreatedAt())
                            .build();
                    
                    log.info("Datos de usuario encontrados para email: {} - {} {}", 
                             email, user.getFirstName(), user.getLastName());
                    
                    ApiResponse<UserDataResponse> response = ApiResponse.success(
                            userData,
                            "Consulta de usuario exitosa"
                    );
                    
                    return ServerResponse.ok().bodyValue(response);
                })
                .switchIfEmpty(
                    ServerResponse.status(HttpStatus.NOT_FOUND)
                            .bodyValue(createErrorResponse("Usuario no encontrado"))
                )
                .onErrorResume(Exception.class, error -> {
                    log.error("Error consultando usuario por email {}: {}", email, error.getMessage(), error);
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .bodyValue(createErrorResponse("Error interno del servidor"));
                });
    }

    /**
     * Valida si un usuario existe y está activo
     * Endpoint rápido para validaciones
     */
    public Mono<ServerResponse> validateUserExists(ServerRequest serverRequest) {
        String document = serverRequest.pathVariable("document");
        
        log.debug("Validando existencia de usuario: {}", document);
        
        return userRepository.findByDocument(document)
                .flatMap(user -> {
                    Map<String, Object> validationResult = Map.of(
                            "exists", true,
                            "isActive", user.isActive(),
                            "emailVerified", true,
                            "document", user.getDocument()
                    );
                    
                    ApiResponse<Map<String, Object>> response = ApiResponse.success(
                            validationResult,
                            "Usuario validado"
                    );
                    
                    return ServerResponse.ok().bodyValue(response);
                })
                .switchIfEmpty(
                    ServerResponse.ok().bodyValue(
                            ApiResponse.success(
                                    Map.of("exists", false, "document", document),
                                    "Usuario no encontrado"
                            )
                    )
                )
                .onErrorResume(Exception.class, error -> {
                    log.error("Error validando usuario {}: {}", document, error.getMessage(), error);
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .bodyValue(createErrorResponse("Error interno del servidor"));
                });
    }

    private ApiResponse<String> createErrorResponse(String message) {
        return ApiResponse.error(message);
    }

    /**
     * DTO para respuesta de datos de usuario (sin información sensible)
     */
    public static class UserDataResponse {
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
        public UserDataResponse() {}

        // Constructor con builder
        private UserDataResponse(Builder builder) {
            this.id = builder.id;
            this.document = builder.document;
            this.email = builder.email;
            this.firstName = builder.firstName;
            this.lastName = builder.lastName;
            this.phone = builder.phone;
            this.baseSalary = builder.baseSalary;
            this.isActive = builder.isActive;
            this.emailVerified = builder.emailVerified;
            this.createdAt = builder.createdAt;
        }

        public static Builder builder() {
            return new Builder();
        }

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

        public static class Builder {
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

            public Builder id(Long id) { this.id = id; return this; }
            public Builder document(String document) { this.document = document; return this; }
            public Builder email(String email) { this.email = email; return this; }
            public Builder firstName(String firstName) { this.firstName = firstName; return this; }
            public Builder lastName(String lastName) { this.lastName = lastName; return this; }
            public Builder phone(String phone) { this.phone = phone; return this; }
            public Builder baseSalary(java.math.BigDecimal baseSalary) { this.baseSalary = baseSalary; return this; }
            public Builder isActive(Boolean isActive) { this.isActive = isActive; return this; }
            public Builder emailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; return this; }
            public Builder createdAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

            public UserDataResponse build() {
                return new UserDataResponse(this);
            }
        }
    }
}
