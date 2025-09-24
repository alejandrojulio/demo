package co.com.pragma.model;

import co.com.pragma.model.auth.LoginRequestDTO;
import co.com.pragma.model.auth.LoginResponseDTO;
import co.com.pragma.model.auth.TokenValidationResponseDTO;
import co.com.pragma.model.user.User;
import co.com.pragma.model.user.UserDTO;
import co.com.pragma.model.user.UserResponseDTO;
import co.com.pragma.model.user.UserRole;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Builder de datos de prueba para AuthService
 * Proporciona métodos para crear objetos de test con datos consistentes
 */
public final class TestDataBuilder {

    private TestDataBuilder() {
        // Utility class
    }

    // ==========================================
    // USER BUILDERS
    // ==========================================

    public static User.UserBuilder defaultUser() {
        return User.builder()
                .id("test-user-123")
                .firstName("Juan")
                .lastName("Pérez")
                .birthDate(LocalDate.of(1990, 5, 15))
                .address("Calle 123 #45-67")
                .phone("+57-301-234-5678")
                .email("juan.perez@test.com")
                .password("$2a$10$hashedPassword123")
                .role(UserRole.CLIENT)
                .isActive(true)
                .document("12345678")
                .baseSalary(new BigDecimal("3000000"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now());
    }

    public static User.UserBuilder adminUser() {
        return defaultUser()
                .id("admin-123")
                .firstName("Admin")
                .lastName("Sistema")
                .email("admin@crediya.com")
                .role(UserRole.ADMIN)
                .document("admin001")
                .baseSalary(new BigDecimal("8000000"));
    }

    public static User.UserBuilder asesorUser() {
        return defaultUser()
                .id("asesor-123")
                .firstName("María")
                .lastName("García")
                .email("asesor@crediya.com")
                .role(UserRole.ADVISOR)
                .document("asesor001")
                .baseSalary(new BigDecimal("5000000"));
    }

    public static User.UserBuilder clienteUser() {
        return defaultUser()
                .id("cliente-123")
                .email("cliente@crediya.com")
                .role(UserRole.CLIENT)
                .document("cliente001");
    }

    public static User.UserBuilder inactiveUser() {
        return defaultUser()
                .isActive(false);
    }

    // ==========================================
    // USER DTO BUILDERS
    // ==========================================

    public static UserDTO.UserDTOBuilder defaultUserDTO() {
        return UserDTO.builder()
                .firstName("Carlos")
                .lastName("Rodríguez")
                .birthDate(LocalDate.of(1985, 8, 20))
                .address("Carrera 456 #78-90")
                .phone("+57-302-345-6789")
                .email("carlos.rodriguez@test.com")
                .password("password123")
                .role(UserRole.CLIENT)
                .document("87654321")
                .baseSalary(new BigDecimal("2500000"));
    }

    public static UserDTO.UserDTOBuilder invalidUserDTO() {
        return defaultUserDTO()
                .email("invalid-email")
                .baseSalary(new BigDecimal("-1000"));
    }

    // ==========================================
    // LOGIN BUILDERS
    // ==========================================

    public static LoginRequestDTO.LoginRequestDTOBuilder defaultLoginRequest() {
        return LoginRequestDTO.builder()
                .email("juan.perez@test.com")
                .password("password123");
    }

    public static LoginRequestDTO.LoginRequestDTOBuilder adminLoginRequest() {
        return LoginRequestDTO.builder()
                .email("admin@crediya.com")
                .password("password123");
    }

    public static LoginRequestDTO.LoginRequestDTOBuilder invalidLoginRequest() {
        return LoginRequestDTO.builder()
                .email("nonexistent@test.com")
                .password("wrongpassword");
    }

    public static LoginRequestDTO.LoginRequestDTOBuilder emptyLoginRequest() {
        return LoginRequestDTO.builder()
                .email("")
                .password("");
    }

    // ==========================================
    // RESPONSE BUILDERS
    // ==========================================

    public static LoginResponseDTO.LoginResponseDTOBuilder successfulLoginResponse() {
        return LoginResponseDTO.builder()
                .success(true)
                .token("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token")
                .id("test-user-123")
                .email("juan.perez@test.com")
                .role(UserRole.CLIENT)
                .message("Authentication successful");
    }

    public static LoginResponseDTO.LoginResponseDTOBuilder failedLoginResponse() {
        return LoginResponseDTO.builder()
                .success(false)
                .message("Invalid credentials");
    }

    public static TokenValidationResponseDTO.TokenValidationResponseDTOBuilder validTokenResponse() {
        return TokenValidationResponseDTO.builder()
                .valid(true)
                .id("test-user-123")
                .email("juan.perez@test.com")
                .role(UserRole.CLIENT)
                .document("12345678")
                .message("Valid token");
    }

    public static TokenValidationResponseDTO.TokenValidationResponseDTOBuilder invalidTokenResponse() {
        return TokenValidationResponseDTO.builder()
                .valid(false)
                .error("Invalid or expired token")
                .message("Access denied");
    }

    public static UserResponseDTO.UserResponseDTOBuilder defaultUserResponse() {
        return UserResponseDTO.builder()
                .id("test-user-123")
                .firstName("Juan")
                .lastName("Pérez")
                .birthDate(LocalDate.of(1990, 5, 15))
                .address("Calle 123 #45-67")
                .phone("+57-301-234-5678")
                .email("juan.perez@test.com")
                    .role(UserRole.CLIENT)
                .isActive(true)
                .document("12345678")
                .baseSalary(new BigDecimal("3000000"));
    }

    // ==========================================
    // CONSTANT VALUES
    // ==========================================

    public static final class TestConstants {
        public static final String VALID_JWT_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.valid.token";
        public static final String INVALID_JWT_TOKEN = "invalid.jwt.token";
        public static final String EXPIRED_JWT_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.expired.token";
        public static final String MALFORMED_JWT_TOKEN = "malformed-token";
        
        public static final String TEST_PASSWORD_RAW = "password123";
        public static final String TEST_PASSWORD_ENCODED = "$2a$10$hashedPassword123";
        
        public static final String VALID_EMAIL = "test@crediya.com";
        public static final String INVALID_EMAIL = "invalid-email";
        public static final String NONEXISTENT_EMAIL = "nonexistent@crediya.com";
        
        public static final String VALID_DOCUMENT = "12345678";
        public static final String INVALID_DOCUMENT = "invalid-doc";
        
        private TestConstants() {}
    }
}
