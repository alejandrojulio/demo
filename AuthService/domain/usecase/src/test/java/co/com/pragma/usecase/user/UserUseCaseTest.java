package co.com.pragma.usecase.user;

import co.com.pragma.model.TestDataBuilder;
import co.com.pragma.model.auth.gateways.PasswordEncoder;
import co.com.pragma.model.user.User;
import co.com.pragma.model.user.UserDTO;
import co.com.pragma.model.user.gateways.ApplicationLogger;
import co.com.pragma.model.user.gateways.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests para UserUseCase
 * Cubre todos los escenarios de creación y consulta de usuarios
 */
@ExtendWith(MockitoExtension.class)
class UserUseCaseTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private ApplicationLogger logger;
    
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserUseCase userUseCase;

    @BeforeEach
    void setUp() {
        userUseCase = new UserUseCase(userRepository, logger, passwordEncoder);
    }

    // ==========================================
    // GET USER TESTS
    // ==========================================

    @Test
    void getUser_WithValidId_ShouldReturnUser() {
        // Given
        String userId = "test-user-123";
        User user = TestDataBuilder.defaultUser().id(userId).build();

        when(userRepository.findById(userId)).thenReturn(Mono.just(user));

        // When & Then
        StepVerifier.create(userUseCase.getuser(userId))
                .expectNextMatches(response -> 
                    response.getId().equals(userId) &&
                    response.getEmail().equals(user.getEmail()) &&
                    response.getFirstName().equals(user.getFirstName()) &&
                    response.getLastName().equals(user.getLastName())
                )
                .verifyComplete();

        verify(userRepository).findById(userId);
    }

    @Test
    void getUser_WithNonexistentId_ShouldReturnEmpty() {
        // Given
        String userId = "nonexistent-user";

        when(userRepository.findById(userId)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(userUseCase.getuser(userId))
                .expectComplete()
                .verify();

        verify(userRepository).findById(userId);
    }

    @Test
    void getUser_WithRepositoryError_ShouldPropagateError() {
        // Given
        String userId = "test-user-123";

        when(userRepository.findById(userId))
                .thenReturn(Mono.error(new RuntimeException("Database connection error")));

        // When & Then
        StepVerifier.create(userUseCase.getuser(userId))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException &&
                    throwable.getMessage().contains("Database connection error")
                )
                .verify();

        verify(userRepository).findById(userId);
    }

    // ==========================================
    // CREATE USER TESTS
    // ==========================================

    @Test
    void createUser_WithValidData_ShouldCreateUser() {
        // Given
        UserDTO userDTO = TestDataBuilder.defaultUserDTO().build();
        User savedUser = TestDataBuilder.defaultUser()
                .email(userDTO.getEmail())
                .firstName(userDTO.getFirstName())
                .lastName(userDTO.getLastName())
                .build();

        when(userRepository.existsByEmail(userDTO.getEmail().toLowerCase().trim()))
                .thenReturn(Mono.just(false));
        when(passwordEncoder.encode(userDTO.getPassword()))
                .thenReturn(TestDataBuilder.TestConstants.TEST_PASSWORD_ENCODED);
        when(userRepository.save(any(User.class)))
                .thenReturn(Mono.just(savedUser));

        // When & Then
        StepVerifier.create(userUseCase.createUser(userDTO))
                .expectNextMatches(response -> 
                    response.getEmail().equals(userDTO.getEmail()) &&
                    response.getFirstName().equals(userDTO.getFirstName()) &&
                    response.getLastName().equals(userDTO.getLastName()) &&
                    response.getBaseSalary().equals(userDTO.getBaseSalary())
                )
                .verifyComplete();

        verify(userRepository).existsByEmail(userDTO.getEmail().toLowerCase().trim());
        verify(passwordEncoder).encode(userDTO.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_WithExistingEmail_ShouldFail() {
        // Given
        UserDTO userDTO = TestDataBuilder.defaultUserDTO().build();

        when(userRepository.existsByEmail(userDTO.getEmail().toLowerCase().trim()))
                .thenReturn(Mono.just(true));

        // When & Then
        StepVerifier.create(userUseCase.createUser(userDTO))
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException &&
                    throwable.getMessage().contains("ya está registrado")
                )
                .verify();

        verify(userRepository).existsByEmail(userDTO.getEmail().toLowerCase().trim());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_WithMissingFirstName_ShouldFail() {
        // Given
        UserDTO userDTO = TestDataBuilder.defaultUserDTO()
                .firstName(null)
                .build();

        // When & Then
        StepVerifier.create(userUseCase.createUser(userDTO))
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException &&
                    throwable.getMessage().contains("nombre es requerido")
                )
                .verify();

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_WithEmptyFirstName_ShouldFail() {
        // Given
        UserDTO userDTO = TestDataBuilder.defaultUserDTO()
                .firstName("   ")
                .build();

        // When & Then
        StepVerifier.create(userUseCase.createUser(userDTO))
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException &&
                    throwable.getMessage().contains("nombre es requerido")
                )
                .verify();
    }

    @Test
    void createUser_WithMissingLastName_ShouldFail() {
        // Given
        UserDTO userDTO = TestDataBuilder.defaultUserDTO()
                .lastName(null)
                .build();

        // When & Then
        StepVerifier.create(userUseCase.createUser(userDTO))
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException &&
                    throwable.getMessage().contains("apellido es requerido")
                )
                .verify();
    }

    @Test
    void createUser_WithMissingEmail_ShouldFail() {
        // Given
        UserDTO userDTO = TestDataBuilder.defaultUserDTO()
                .email(null)
                .build();

        // When & Then
        StepVerifier.create(userUseCase.createUser(userDTO))
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException &&
                    throwable.getMessage().contains("email es requerido")
                )
                .verify();
    }

    @Test
    void createUser_WithMissingBaseSalary_ShouldFail() {
        // Given
        UserDTO userDTO = TestDataBuilder.defaultUserDTO()
                .baseSalary(null)
                .build();

        // When & Then
        StepVerifier.create(userUseCase.createUser(userDTO))
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException &&
                    throwable.getMessage().contains("salario base es requerido")
                )
                .verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "invalid-email", 
        "test@", 
        "@domain.com", 
        "test.domain.com", 
        "test@domain", 
        "test @domain.com"
    })
    void createUser_WithInvalidEmailFormat_ShouldFail(String invalidEmail) {
        // Given
        UserDTO userDTO = TestDataBuilder.defaultUserDTO()
                .email(invalidEmail)
                .build();

        // When & Then
        StepVerifier.create(userUseCase.createUser(userDTO))
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException &&
                    throwable.getMessage().contains("formato de email inválido")
                )
                .verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "test@domain.com", 
        "user.name@company.co", 
        "user+tag@example.org",
        "test123@domain-name.com"
    })
    void createUser_WithValidEmailFormats_ShouldPass(String validEmail) {
        // Given
        UserDTO userDTO = TestDataBuilder.defaultUserDTO()
                .email(validEmail)
                .build();
        User savedUser = TestDataBuilder.defaultUser()
                .email(validEmail)
                .build();

        when(userRepository.existsByEmail(validEmail.toLowerCase().trim()))
                .thenReturn(Mono.just(false));
        when(passwordEncoder.encode(userDTO.getPassword()))
                .thenReturn(TestDataBuilder.TestConstants.TEST_PASSWORD_ENCODED);
        when(userRepository.save(any(User.class)))
                .thenReturn(Mono.just(savedUser));

        // When & Then
        StepVerifier.create(userUseCase.createUser(userDTO))
                .expectNextMatches(response -> response.getEmail().equals(validEmail))
                .verifyComplete();
    }

    @Test
    void createUser_WithNegativeSalary_ShouldFail() {
        // Given
        UserDTO userDTO = TestDataBuilder.defaultUserDTO()
                .baseSalary(new BigDecimal("-1000"))
                .build();

        // When & Then
        StepVerifier.create(userUseCase.createUser(userDTO))
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException &&
                    throwable.getMessage().contains("salario debe ser mayor")
                )
                .verify();
    }

    @Test
    void createUser_WithSalaryAboveMaximum_ShouldFail() {
        // Given
        UserDTO userDTO = TestDataBuilder.defaultUserDTO()
                .baseSalary(new BigDecimal("20000000")) // Above max 15M
                .build();

        // When & Then
        StepVerifier.create(userUseCase.createUser(userDTO))
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException &&
                    throwable.getMessage().contains("salario debe ser menor")
                )
                .verify();
    }

    @Test
    void createUser_WithBoundarySalaries_ShouldWork() {
        // Test minimum salary (0)
        testCreateUserWithSalary(BigDecimal.ZERO);
        
        // Test maximum salary (15M)
        testCreateUserWithSalary(new BigDecimal("15000000"));
        
        // Test valid mid-range salary
        testCreateUserWithSalary(new BigDecimal("5000000"));
    }

    @Test
    void createUser_WithEmailCaseVariations_ShouldNormalizeEmail() {
        // Given
        UserDTO userDTO = TestDataBuilder.defaultUserDTO()
                .email("TEST.USER@DOMAIN.COM")
                .build();
        User savedUser = TestDataBuilder.defaultUser()
                .email("test.user@domain.com")
                .build();

        when(userRepository.existsByEmail("test.user@domain.com"))
                .thenReturn(Mono.just(false));
        when(passwordEncoder.encode(userDTO.getPassword()))
                .thenReturn(TestDataBuilder.TestConstants.TEST_PASSWORD_ENCODED);
        when(userRepository.save(any(User.class)))
                .thenReturn(Mono.just(savedUser));

        // When & Then
        StepVerifier.create(userUseCase.createUser(userDTO))
                .expectNextMatches(response -> true)
                .verifyComplete();

        verify(userRepository).existsByEmail("test.user@domain.com");
    }

    @Test
    void createUser_WithDatabaseErrorDuringEmailCheck_ShouldPropagateError() {
        // Given
        UserDTO userDTO = TestDataBuilder.defaultUserDTO().build();

        when(userRepository.existsByEmail(userDTO.getEmail().toLowerCase().trim()))
                .thenReturn(Mono.error(new RuntimeException("Database error")));

        // When & Then
        StepVerifier.create(userUseCase.createUser(userDTO))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException &&
                    throwable.getMessage().contains("Database error")
                )
                .verify();
    }

    @Test
    void createUser_WithDatabaseErrorDuringSave_ShouldPropagateError() {
        // Given
        UserDTO userDTO = TestDataBuilder.defaultUserDTO().build();

        when(userRepository.existsByEmail(userDTO.getEmail().toLowerCase().trim()))
                .thenReturn(Mono.just(false));
        when(passwordEncoder.encode(userDTO.getPassword()))
                .thenReturn(TestDataBuilder.TestConstants.TEST_PASSWORD_ENCODED);
        when(userRepository.save(any(User.class)))
                .thenReturn(Mono.error(new RuntimeException("Save error")));

        // When & Then
        StepVerifier.create(userUseCase.createUser(userDTO))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException &&
                    throwable.getMessage().contains("Save error")
                )
                .verify();
    }

    @Test
    void createUser_ShouldEncryptPassword() {
        // Given
        UserDTO userDTO = TestDataBuilder.defaultUserDTO().build();
        User savedUser = TestDataBuilder.defaultUser().build();

        when(userRepository.existsByEmail(userDTO.getEmail().toLowerCase().trim()))
                .thenReturn(Mono.just(false));
        when(passwordEncoder.encode(userDTO.getPassword()))
                .thenReturn("encrypted-password");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User userToSave = invocation.getArgument(0);
                    // Verificar que la contraseña está encriptada
                    if (!"encrypted-password".equals(userToSave.getPassword())) {
                        throw new RuntimeException("Password not encrypted");
                    }
                    return Mono.just(savedUser);
                });

        // When & Then
        StepVerifier.create(userUseCase.createUser(userDTO))
                .expectNextMatches(response -> response.getId() != null)
                .verifyComplete();

        verify(passwordEncoder).encode(userDTO.getPassword());
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private void testCreateUserWithSalary(BigDecimal salary) {
        // Given
        UserDTO userDTO = TestDataBuilder.defaultUserDTO()
                .baseSalary(salary)
                .email("test" + salary.intValue() + "@test.com") // Unique email for each test
                .build();
        User savedUser = TestDataBuilder.defaultUser()
                .baseSalary(salary)
                .build();

        when(userRepository.existsByEmail(anyString()))
                .thenReturn(Mono.just(false));
        when(passwordEncoder.encode(userDTO.getPassword()))
                .thenReturn(TestDataBuilder.TestConstants.TEST_PASSWORD_ENCODED);
        when(userRepository.save(any(User.class)))
                .thenReturn(Mono.just(savedUser));

        // When & Then
        StepVerifier.create(userUseCase.createUser(userDTO))
                .expectNextMatches(response -> response.getBaseSalary().equals(salary))
                .verifyComplete();

        // Reset mocks for next test
        reset(userRepository, passwordEncoder);
    }
}
