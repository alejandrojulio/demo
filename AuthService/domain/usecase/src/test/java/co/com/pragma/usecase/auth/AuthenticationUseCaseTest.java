package co.com.pragma.usecase.auth;

import co.com.pragma.model.TestDataBuilder;
import co.com.pragma.model.auth.LoginRequestDTO;
import co.com.pragma.model.auth.LoginResponseDTO;
import co.com.pragma.model.auth.TokenValidationResponseDTO;
import co.com.pragma.model.auth.gateways.JwtTokenGenerator;
import co.com.pragma.model.auth.gateways.PasswordEncoder;
import co.com.pragma.model.user.User;
import co.com.pragma.model.user.UserRole;
import co.com.pragma.model.user.gateways.ApplicationLogger;
import co.com.pragma.model.user.gateways.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests para AuthenticationUseCase
 * Cubre todos los escenarios de autenticación y validación de tokens
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationUseCaseTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtTokenGenerator jwtTokenGenerator;
    
    @Mock
    private ApplicationLogger logger;

    private AuthenticationUseCase authenticationUseCase;

    @BeforeEach
    void setUp() {
        authenticationUseCase = new AuthenticationUseCase(
            userRepository, 
            passwordEncoder,
            jwtTokenGenerator,
            logger
        );
    }

    // ==========================================
    // LOGIN TESTS
    // ==========================================

    @Test
    void login_WithValidCredentials_ShouldReturnSuccessfulResponse() {
        // Given
        LoginRequestDTO loginRequest = TestDataBuilder.defaultLoginRequest().build();
        User user = TestDataBuilder.defaultUser().build();
        String token = TestDataBuilder.TestConstants.VALID_JWT_TOKEN;

        when(userRepository.findByEmail(loginRequest.getEmail().toLowerCase().trim()))
                .thenReturn(Mono.just(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword()))
                .thenReturn(true);
        when(jwtTokenGenerator.generateToken(user.getId(), user.getEmail(), user.getRole(), user.getDocument()))
                .thenReturn(token);

        // When & Then
        StepVerifier.create(authenticationUseCase.login(loginRequest))
                .expectNextMatches(response -> 
                    response.isSuccess() &&
                    response.getToken().equals(token) &&
                    response.getEmail().equals(user.getEmail()) &&
                    response.getRole().equals(user.getRole()) &&
                    response.getId().equals(user.getId())
                )
                .verifyComplete();

        verify(userRepository).findByEmail(loginRequest.getEmail().toLowerCase().trim());
        verify(passwordEncoder).matches(loginRequest.getPassword(), user.getPassword());
        verify(jwtTokenGenerator).generateToken(user.getId(), user.getEmail(), user.getRole(), user.getDocument());
    }

    @Test
    void login_WithNonexistentUser_ShouldReturnFailureResponse() {
        // Given
        LoginRequestDTO loginRequest = TestDataBuilder.invalidLoginRequest().build();

        when(userRepository.findByEmail(loginRequest.getEmail().toLowerCase().trim()))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(authenticationUseCase.login(loginRequest))
                .expectNextMatches(response -> 
                    !response.isSuccess() &&
                    response.getToken() == null &&
                    response.getMessage().contains("credentials")
                )
                .verifyComplete();

        verify(userRepository).findByEmail(loginRequest.getEmail().toLowerCase().trim());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtTokenGenerator, never()).generateToken(anyString(), anyString(), any(UserRole.class), anyString());
    }

    @Test
    void login_WithInvalidPassword_ShouldReturnFailureResponse() {
        // Given
        LoginRequestDTO loginRequest = TestDataBuilder.defaultLoginRequest().build();
        User user = TestDataBuilder.defaultUser().build();

        when(userRepository.findByEmail(loginRequest.getEmail().toLowerCase().trim()))
                .thenReturn(Mono.just(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword()))
                .thenReturn(false);

        // When & Then
        StepVerifier.create(authenticationUseCase.login(loginRequest))
                .expectNextMatches(response -> 
                    !response.isSuccess() &&
                    response.getMessage().contains("server error")
                )
                .verifyComplete();

        verify(userRepository).findByEmail(loginRequest.getEmail().toLowerCase().trim());
        verify(passwordEncoder).matches(loginRequest.getPassword(), user.getPassword());
        verify(jwtTokenGenerator, never()).generateToken(anyString(), anyString(), any(UserRole.class), anyString());
    }

    @Test
    void login_WithDatabaseError_ShouldReturnErrorResponse() {
        // Given
        LoginRequestDTO loginRequest = TestDataBuilder.defaultLoginRequest().build();

        when(userRepository.findByEmail(loginRequest.getEmail().toLowerCase().trim()))
                .thenReturn(Mono.error(new RuntimeException("Database connection error")));

        // When & Then
        StepVerifier.create(authenticationUseCase.login(loginRequest))
                .expectNextMatches(response -> 
                    !response.isSuccess() &&
                    response.getMessage().contains("server error")
                )
                .verifyComplete();

        verify(userRepository).findByEmail(loginRequest.getEmail().toLowerCase().trim());
    }

    @Test
    void login_WithEmailCaseVariations_ShouldNormalizeEmail() {
        // Given
        LoginRequestDTO loginRequest = LoginRequestDTO.builder()
                .email("JUAN.PEREZ@TEST.COM")
                .password(TestDataBuilder.TestConstants.TEST_PASSWORD_RAW)
                .build();
        User user = TestDataBuilder.defaultUser().build();
        String token = TestDataBuilder.TestConstants.VALID_JWT_TOKEN;

        when(userRepository.findByEmail("juan.perez@test.com"))
                .thenReturn(Mono.just(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword()))
                .thenReturn(true);
        when(jwtTokenGenerator.generateToken(user.getId(), user.getEmail(), user.getRole(), user.getDocument()))
                .thenReturn(token);

        // When & Then
        StepVerifier.create(authenticationUseCase.login(loginRequest))
                .expectNextMatches(LoginResponseDTO::isSuccess)
                .verifyComplete();

        verify(userRepository).findByEmail("juan.perez@test.com");
    }

    // ==========================================
    // TOKEN VALIDATION TESTS
    // ==========================================

    @Test
    void validateToken_WithValidToken_ShouldReturnValidResponse() {
        // Given
        String token = TestDataBuilder.TestConstants.VALID_JWT_TOKEN;
        String userId = "test-user-123";
        String email = TestDataBuilder.TestConstants.VALID_EMAIL;
        UserRole role = UserRole.CLIENT;
        String document = TestDataBuilder.TestConstants.VALID_DOCUMENT;

        when(jwtTokenGenerator.isTokenValid(token)).thenReturn(true);
        when(jwtTokenGenerator.extractUserId(token)).thenReturn(userId);
        when(jwtTokenGenerator.extractEmail(token)).thenReturn(email);
        when(jwtTokenGenerator.extractRole(token)).thenReturn(role);
        when(jwtTokenGenerator.extractDocument(token)).thenReturn(document);

        // When & Then
        StepVerifier.create(authenticationUseCase.validateToken(token))
                .expectNextMatches(response -> 
                    response.isValid() &&
                    response.getId().equals(userId) &&
                    response.getEmail().equals(email) &&
                    response.getRole().equals(role) &&
                    response.getDocument().equals(document)
                )
                .verifyComplete();

        verify(jwtTokenGenerator).isTokenValid(token);
        verify(jwtTokenGenerator).extractUserId(token);
        verify(jwtTokenGenerator).extractEmail(token);
        verify(jwtTokenGenerator).extractRole(token);
        verify(jwtTokenGenerator).extractDocument(token);
    }

    @Test
    void validateToken_WithInvalidToken_ShouldReturnInvalidResponse() {
        // Given
        String token = TestDataBuilder.TestConstants.INVALID_JWT_TOKEN;

        when(jwtTokenGenerator.isTokenValid(token)).thenReturn(false);

        // When & Then
        StepVerifier.create(authenticationUseCase.validateToken(token))
                .expectNextMatches(response -> 
                    !response.isValid() &&
                    response.getError() != null &&
                    response.getMessage().contains("Access denied")
                )
                .verifyComplete();

        verify(jwtTokenGenerator).isTokenValid(token);
        verify(jwtTokenGenerator, never()).extractUserId(anyString());
    }

    @Test
    void validateToken_WithMalformedToken_ShouldReturnInvalidResponse() {
        // Given
        String token = TestDataBuilder.TestConstants.MALFORMED_JWT_TOKEN;

        when(jwtTokenGenerator.isTokenValid(token)).thenReturn(true);
        when(jwtTokenGenerator.extractUserId(token))
                .thenThrow(new RuntimeException("Invalid token format"));

        // When & Then
        StepVerifier.create(authenticationUseCase.validateToken(token))
                .expectNextMatches(response -> 
                    !response.isValid() &&
                    response.getError() != null
                )
                .verifyComplete();

        verify(jwtTokenGenerator).isTokenValid(token);
        verify(jwtTokenGenerator).extractUserId(token);
    }

    @Test
    void validateToken_WithExpiredToken_ShouldReturnInvalidResponse() {
        // Given
        String token = TestDataBuilder.TestConstants.EXPIRED_JWT_TOKEN;

        when(jwtTokenGenerator.isTokenValid(token)).thenReturn(false);

        // When & Then
        StepVerifier.create(authenticationUseCase.validateToken(token))
                .expectNextMatches(response -> !response.isValid())
                .verifyComplete();

        verify(jwtTokenGenerator).isTokenValid(token);
    }

    @Test
    void validateToken_WithUnexpectedException_ShouldReturnErrorResponse() {
        // Given
        String token = TestDataBuilder.TestConstants.VALID_JWT_TOKEN;

        when(jwtTokenGenerator.isTokenValid(token))
                .thenThrow(new OutOfMemoryError("System error"));

        // When & Then
        StepVerifier.create(authenticationUseCase.validateToken(token))
                .expectNextMatches(response -> 
                    !response.isValid() &&
                    response.getError() != null &&
                    response.getMessage().contains("error de validación")
                )
                .verifyComplete();

        verify(jwtTokenGenerator).isTokenValid(token);
    }

    // ==========================================
    // INTEGRATION TESTS
    // ==========================================

    @Test
    void loginAndValidateToken_FullFlow_ShouldWork() {
        // Given
        LoginRequestDTO loginRequest = TestDataBuilder.defaultLoginRequest().build();
        User user = TestDataBuilder.defaultUser().build();
        String token = TestDataBuilder.TestConstants.VALID_JWT_TOKEN;

        // Login mocks
        when(userRepository.findByEmail(loginRequest.getEmail().toLowerCase().trim()))
                .thenReturn(Mono.just(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword()))
                .thenReturn(true);
        when(jwtTokenGenerator.generateToken(user.getId(), user.getEmail(), user.getRole(), user.getDocument()))
                .thenReturn(token);

        // Token validation mocks
        when(jwtTokenGenerator.isTokenValid(token)).thenReturn(true);
        when(jwtTokenGenerator.extractUserId(token)).thenReturn(user.getId());
        when(jwtTokenGenerator.extractEmail(token)).thenReturn(user.getEmail());
        when(jwtTokenGenerator.extractRole(token)).thenReturn(user.getRole());
        when(jwtTokenGenerator.extractDocument(token)).thenReturn(user.getDocument());

        // When & Then
        StepVerifier.create(
                authenticationUseCase.login(loginRequest)
                        .flatMap(loginResponse -> {
                            if (loginResponse.isSuccess()) {
                                return authenticationUseCase.validateToken(loginResponse.getToken());
                            } else {
                                return Mono.error(new RuntimeException("Login failed"));
                            }
                        })
        )
                .expectNextMatches(TokenValidationResponseDTO::isValid)
                .verifyComplete();
    }

    @Test
    void login_WithAllUserRoles_ShouldWork() {
        // Test for CLIENT
        testLoginWithRole(UserRole.CLIENT);
        
        // Test for ADVISOR
        testLoginWithRole(UserRole.ADVISOR);
        
        // Test for ADMIN
        testLoginWithRole(UserRole.ADMIN);
    }

    private void testLoginWithRole(UserRole role) {
        // Given
        LoginRequestDTO loginRequest = TestDataBuilder.defaultLoginRequest().build();
        User user = TestDataBuilder.defaultUser().role(role).build();
        String token = TestDataBuilder.TestConstants.VALID_JWT_TOKEN;

        when(userRepository.findByEmail(loginRequest.getEmail().toLowerCase().trim()))
                .thenReturn(Mono.just(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword()))
                .thenReturn(true);
        when(jwtTokenGenerator.generateToken(user.getId(), user.getEmail(), user.getRole(), user.getDocument()))
                .thenReturn(token);

        // When & Then
        StepVerifier.create(authenticationUseCase.login(loginRequest))
                .expectNextMatches(response -> 
                    response.isSuccess() && 
                    response.getRole().equals(role)
                )
                .verifyComplete();

        // Reset mocks for next test
        reset(userRepository, passwordEncoder, jwtTokenGenerator);
    }
}
