package co.com.pragma.usecase.loan;

import co.com.pragma.model.TestDataBuilder;
import co.com.pragma.model.loan.LoanRequest;
import co.com.pragma.model.loan.LoanRequestReviewDTO;
import co.com.pragma.model.loan.gateways.LoanApplicationLogger;
import co.com.pragma.model.loan.gateways.LoanRequestRepository;
import co.com.pragma.model.user.UserData;
import co.com.pragma.model.user.gateways.UserDataGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests para ListLoanRequestsForReviewUseCase
 * Cubre la funcionalidad de listar solicitudes que requieren revisión manual
 */
@ExtendWith(MockitoExtension.class)
class ListLoanRequestsForReviewUseCaseTest {

    @Mock
    private LoanRequestRepository loanRequestRepository;
    
    @Mock
    private LoanApplicationLogger logger;
    
    @Mock
    private UserDataGateway userDataGateway;

    private ListLoanRequestsForReviewUseCase useCase;

    // Estados que requieren revisión manual
    private static final List<String> ESTADOS_REVISION_MANUAL = List.of(
            LoanRequest.LoanStatus.PENDING_REVIEW.name(),
            LoanRequest.LoanStatus.REJECTED.name(),
            LoanRequest.LoanStatus.MANUAL_REVIEW.name()
    );

    @BeforeEach
    void setUp() {
        useCase = new ListLoanRequestsForReviewUseCase(
            loanRequestRepository, 
            logger, 
            userDataGateway
        );
    }

    // ==========================================
    // SUCCESSFUL QUERY TESTS
    // ==========================================

    @Test
    void listSolicitudesForReview_WithValidParameters_ShouldReturnEnrichedData() {
        // Given
        int page = 0;
        int size = 10;
        String userEmail = TestDataBuilder.SecurityContextBuilder.ADVISOR_EMAIL;
        
        LoanRequestReviewDTO reviewDTO = TestDataBuilder.defaultReviewDTO()
                .documentoCliente("12345678")
                .build();
        
        UserData userData = UserData.builder()
                .email("cliente@test.com")
                .firstName("Juan")
                .lastName("Pérez")
                .baseSalary(new BigDecimal("3000000"))
                .build();

        when(loanRequestRepository.findSolicitudesForManualReview(page, size, ESTADOS_REVISION_MANUAL))
                .thenReturn(Flux.just(reviewDTO));
        when(userDataGateway.getUserByDocument("12345678"))
                .thenReturn(Mono.just(userData));

        // When & Then
        StepVerifier.create(useCase.listSolicitudesForReview(page, size, userEmail))
                .expectNextMatches(result -> 
                    result.getEmail().equals(userData.getEmail()) &&
                    result.getNombre().equals(userData.getFirstName() + " " + userData.getLastName()) &&
                    result.getSalarioBase().equals(userData.getBaseSalary()) &&
                    result.getDocumentoCliente().equals("12345678")
                )
                .verifyComplete();

        verify(loanRequestRepository).findSolicitudesForManualReview(page, size, ESTADOS_REVISION_MANUAL);
        verify(userDataGateway).getUserByDocument("12345678");
    }

    @Test
    void listSolicitudesForReview_WithMultipleResults_ShouldEnrichAll() {
        // Given
        int page = 0;
        int size = 10;
        String userEmail = TestDataBuilder.SecurityContextBuilder.ADVISOR_EMAIL;
        
        LoanRequestReviewDTO reviewDTO1 = TestDataBuilder.defaultReviewDTO()
                .id(1L)
                .documentoCliente("12345678")
                .build();
        
        LoanRequestReviewDTO reviewDTO2 = TestDataBuilder.defaultReviewDTO()
                .id(2L)
                .documentoCliente("87654321")
                .build();
        
        UserData userData1 = createUserData("cliente1@test.com", "Juan", "Pérez");
        UserData userData2 = createUserData("cliente2@test.com", "María", "García");

        when(loanRequestRepository.findSolicitudesForManualReview(page, size, ESTADOS_REVISION_MANUAL))
                .thenReturn(Flux.just(reviewDTO1, reviewDTO2));
        when(userDataGateway.getUserByDocument("12345678"))
                .thenReturn(Mono.just(userData1));
        when(userDataGateway.getUserByDocument("87654321"))
                .thenReturn(Mono.just(userData2));

        // When & Then
        StepVerifier.create(useCase.listSolicitudesForReview(page, size, userEmail))
                .expectNextCount(2)
                .verifyComplete();

        verify(loanRequestRepository).findSolicitudesForManualReview(page, size, ESTADOS_REVISION_MANUAL);
        verify(userDataGateway).getUserByDocument("12345678");
        verify(userDataGateway).getUserByDocument("87654321");
    }

    @Test
    void listSolicitudesForReview_WithUserDataError_ShouldReturnOriginalDTO() {
        // Given
        int page = 0;
        int size = 10;
        String userEmail = TestDataBuilder.SecurityContextBuilder.ADVISOR_EMAIL;
        
        LoanRequestReviewDTO reviewDTO = TestDataBuilder.defaultReviewDTO()
                .documentoCliente("12345678")
                .email(null) // Sin datos de usuario
                .nombre(null)
                .salarioBase(null)
                .build();

        when(loanRequestRepository.findSolicitudesForManualReview(page, size, ESTADOS_REVISION_MANUAL))
                .thenReturn(Flux.just(reviewDTO));
        when(userDataGateway.getUserByDocument("12345678"))
                .thenReturn(Mono.error(new RuntimeException("User service error")));

        // When & Then
        StepVerifier.create(useCase.listSolicitudesForReview(page, size, userEmail))
                .expectNextMatches(result -> 
                    result.getEmail() == null &&
                    result.getNombre() == null &&
                    result.getSalarioBase() == null &&
                    result.getDocumentoCliente().equals("12345678")
                )
                .verifyComplete();

        verify(userDataGateway).getUserByDocument("12345678");
    }

    // ==========================================
    // VALIDATION TESTS
    // ==========================================

    @ParameterizedTest
    @ValueSource(ints = {-1, -5, -10})
    void listSolicitudesForReview_WithInvalidPage_ShouldFail(int invalidPage) {
        // Given
        int size = 10;
        String userEmail = TestDataBuilder.SecurityContextBuilder.ADVISOR_EMAIL;

        // When & Then
        StepVerifier.create(useCase.listSolicitudesForReview(invalidPage, size, userEmail))
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException &&
                    throwable.getMessage().contains("página debe ser mayor o igual a 0")
                )
                .verify();

        verify(loanRequestRepository, never()).findSolicitudesForManualReview(anyInt(), anyInt(), anyList());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 101, 150})
    void listSolicitudesForReview_WithInvalidSize_ShouldFail(int invalidSize) {
        // Given
        int page = 0;
        String userEmail = TestDataBuilder.SecurityContextBuilder.ADVISOR_EMAIL;

        // When & Then
        StepVerifier.create(useCase.listSolicitudesForReview(page, invalidSize, userEmail))
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException &&
                    throwable.getMessage().contains("tamaño de página debe estar entre 1 y 100")
                )
                .verify();

        verify(loanRequestRepository, never()).findSolicitudesForManualReview(anyInt(), anyInt(), anyList());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10, 50, 100})
    void listSolicitudesForReview_WithValidSizes_ShouldWork(int validSize) {
        // Given
        int page = 0;
        String userEmail = TestDataBuilder.SecurityContextBuilder.ADVISOR_EMAIL;

        when(loanRequestRepository.findSolicitudesForManualReview(page, validSize, ESTADOS_REVISION_MANUAL))
                .thenReturn(Flux.empty());

        // When & Then
        StepVerifier.create(useCase.listSolicitudesForReview(page, validSize, userEmail))
                .verifyComplete();

        verify(loanRequestRepository).findSolicitudesForManualReview(page, validSize, ESTADOS_REVISION_MANUAL);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 5, 10})
    void listSolicitudesForReview_WithValidPages_ShouldWork(int validPage) {
        // Given
        int size = 10;
        String userEmail = TestDataBuilder.SecurityContextBuilder.ADVISOR_EMAIL;

        when(loanRequestRepository.findSolicitudesForManualReview(validPage, size, ESTADOS_REVISION_MANUAL))
                .thenReturn(Flux.empty());

        // When & Then
        StepVerifier.create(useCase.listSolicitudesForReview(validPage, size, userEmail))
                .verifyComplete();

        verify(loanRequestRepository).findSolicitudesForManualReview(validPage, size, ESTADOS_REVISION_MANUAL);
    }

    // ==========================================
    // COUNT TESTS
    // ==========================================

    @Test
    void countSolicitudesForReview_WithValidEmail_ShouldReturnCount() {
        // Given
        String userEmail = TestDataBuilder.SecurityContextBuilder.ADVISOR_EMAIL;
        Long expectedCount = 15L;

        when(loanRequestRepository.countSolicitudesForManualReview(ESTADOS_REVISION_MANUAL))
                .thenReturn(Mono.just(expectedCount));

        // When & Then
        StepVerifier.create(useCase.countSolicitudesForReview(userEmail))
                .expectNext(expectedCount)
                .verifyComplete();

        verify(loanRequestRepository).countSolicitudesForManualReview(ESTADOS_REVISION_MANUAL);
    }

    @Test
    void countSolicitudesForReview_WithRepositoryError_ShouldPropagateError() {
        // Given
        String userEmail = TestDataBuilder.SecurityContextBuilder.ADVISOR_EMAIL;

        when(loanRequestRepository.countSolicitudesForManualReview(ESTADOS_REVISION_MANUAL))
                .thenReturn(Mono.error(new RuntimeException("Database error")));

        // When & Then
        StepVerifier.create(useCase.countSolicitudesForReview(userEmail))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException &&
                    throwable.getMessage().contains("Database error")
                )
                .verify();
    }

    @Test
    void countSolicitudesForReview_WithZeroResults_ShouldReturnZero() {
        // Given
        String userEmail = TestDataBuilder.SecurityContextBuilder.ADVISOR_EMAIL;

        when(loanRequestRepository.countSolicitudesForManualReview(ESTADOS_REVISION_MANUAL))
                .thenReturn(Mono.just(0L));

        // When & Then
        StepVerifier.create(useCase.countSolicitudesForReview(userEmail))
                .expectNext(0L)
                .verifyComplete();
    }

    // ==========================================
    // REPOSITORY ERROR TESTS
    // ==========================================

    @Test
    void listSolicitudesForReview_WithRepositoryError_ShouldPropagateError() {
        // Given
        int page = 0;
        int size = 10;
        String userEmail = TestDataBuilder.SecurityContextBuilder.ADVISOR_EMAIL;

        when(loanRequestRepository.findSolicitudesForManualReview(page, size, ESTADOS_REVISION_MANUAL))
                .thenReturn(Flux.error(new RuntimeException("Database connection error")));

        // When & Then
        StepVerifier.create(useCase.listSolicitudesForReview(page, size, userEmail))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException &&
                    throwable.getMessage().contains("Database connection error")
                )
                .verify();
    }

    @Test
    void listSolicitudesForReview_WithEmptyResults_ShouldCompleteEmpty() {
        // Given
        int page = 0;
        int size = 10;
        String userEmail = TestDataBuilder.SecurityContextBuilder.ADVISOR_EMAIL;

        when(loanRequestRepository.findSolicitudesForManualReview(page, size, ESTADOS_REVISION_MANUAL))
                .thenReturn(Flux.empty());

        // When & Then
        StepVerifier.create(useCase.listSolicitudesForReview(page, size, userEmail))
                .verifyComplete();
    }

    // ==========================================
    // EDGE CASES TESTS
    // ==========================================

    @Test
    void listSolicitudesForReview_WithMaximumPageSize_ShouldWork() {
        // Given
        int page = 0;
        int size = 100; // Maximum allowed size
        String userEmail = TestDataBuilder.SecurityContextBuilder.ADVISOR_EMAIL;

        when(loanRequestRepository.findSolicitudesForManualReview(page, size, ESTADOS_REVISION_MANUAL))
                .thenReturn(Flux.empty());

        // When & Then
        StepVerifier.create(useCase.listSolicitudesForReview(page, size, userEmail))
                .verifyComplete();

        verify(loanRequestRepository).findSolicitudesForManualReview(page, size, ESTADOS_REVISION_MANUAL);
    }

    @Test
    void listSolicitudesForReview_WithLargePageNumber_ShouldWork() {
        // Given
        int page = 999;
        int size = 10;
        String userEmail = TestDataBuilder.SecurityContextBuilder.ADVISOR_EMAIL;

        when(loanRequestRepository.findSolicitudesForManualReview(page, size, ESTADOS_REVISION_MANUAL))
                .thenReturn(Flux.empty());

        // When & Then
        StepVerifier.create(useCase.listSolicitudesForReview(page, size, userEmail))
                .verifyComplete();

        verify(loanRequestRepository).findSolicitudesForManualReview(page, size, ESTADOS_REVISION_MANUAL);
    }

    @Test
    void listSolicitudesForReview_WithPartialUserDataFailure_ShouldContinue() {
        // Given
        int page = 0;
        int size = 10;
        String userEmail = TestDataBuilder.SecurityContextBuilder.ADVISOR_EMAIL;
        
        LoanRequestReviewDTO reviewDTO1 = TestDataBuilder.defaultReviewDTO()
                .id(1L)
                .documentoCliente("12345678")
                .build();
        
        LoanRequestReviewDTO reviewDTO2 = TestDataBuilder.defaultReviewDTO()
                .id(2L)
                .documentoCliente("87654321")
                .build();
        
        UserData userData1 = createUserData("cliente1@test.com", "Juan", "Pérez");

        when(loanRequestRepository.findSolicitudesForManualReview(page, size, ESTADOS_REVISION_MANUAL))
                .thenReturn(Flux.just(reviewDTO1, reviewDTO2));
        when(userDataGateway.getUserByDocument("12345678"))
                .thenReturn(Mono.just(userData1));
        when(userDataGateway.getUserByDocument("87654321"))
                .thenReturn(Mono.error(new RuntimeException("User not found")));

        // When & Then
        StepVerifier.create(useCase.listSolicitudesForReview(page, size, userEmail))
                .expectNextMatches(result -> result.getDocumentoCliente().equals("12345678"))
                .expectNextMatches(result -> result.getDocumentoCliente().equals("87654321"))
                .verifyComplete();
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private UserData createUserData(String email, String firstName, String lastName) {
        return UserData.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .baseSalary(new BigDecimal("3000000"))
                .build();
    }
}
