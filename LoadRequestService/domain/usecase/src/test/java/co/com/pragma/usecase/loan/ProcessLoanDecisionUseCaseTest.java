package co.com.pragma.usecase.loan;

import co.com.pragma.model.TestDataBuilder;
import co.com.pragma.model.loan.LoanDecisionDTO;
import co.com.pragma.model.loan.LoanRequest;
import co.com.pragma.model.loan.gateways.LoanApplicationLogger;
import co.com.pragma.model.loan.gateways.LoanRequestRepository;
import co.com.pragma.usecase.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests para ProcessLoanDecisionUseCase
 * Cubre todos los escenarios de procesamiento de decisiones de préstamo
 */
@ExtendWith(MockitoExtension.class)
class ProcessLoanDecisionUseCaseTest {

    @Mock
    private LoanRequestRepository loanRequestRepository;
    
    @Mock
    private LoanApplicationLogger logger;
    
    @Mock
    private NotificationService notificationService;

    private ProcessLoanDecisionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ProcessLoanDecisionUseCase(
            loanRequestRepository, 
            logger, 
            notificationService
        );
    }

    // ==========================================
    // APPROVAL DECISION TESTS
    // ==========================================

    @Test
    void processDecision_WithValidApproval_ShouldApproveAndNotify() {
        // Given
        LoanDecisionDTO decision = TestDataBuilder.approvalDecision().build();
        LoanRequest existingRequest = TestDataBuilder.defaultLoanRequest()
                .id(decision.getSolicitudId())
                .status(LoanRequest.LoanStatus.PENDING_REVIEW)
                .build();
        when(loanRequestRepository.findById(decision.getSolicitudId()))
                .thenReturn(Mono.just(existingRequest));
        when(loanRequestRepository.save(any(LoanRequest.class)))
                .thenAnswer(invocation -> {
                    LoanRequest updated = invocation.getArgument(0);
                    return Mono.just(updated);
                });
        when(notificationService.sendLoanDecisionNotification(any(LoanRequest.class), anyString()))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(useCase.processDecision(decision))
                .expectNextMatches(result -> 
                    result.getStatus() == LoanRequest.LoanStatus.APPROVED &&
                    result.getApprovedAmount().equals(decision.getMontoAprobado()) &&
                    result.getInterestRate().equals(decision.getTasaInteres()) &&
                    result.getTermInMonths().equals(decision.getPlazoAprobado()) &&
                    result.getMonthlyPayment() != null &&
                    result.getApprovedAt() != null
                )
                .verifyComplete();

        verify(loanRequestRepository).findById(decision.getSolicitudId());
        verify(loanRequestRepository).save(any(LoanRequest.class));
        verify(notificationService).sendLoanDecisionNotification(any(LoanRequest.class), eq(decision.getAsesorEmail()));
    }

    @Test
    void processDecision_WithValidRejection_ShouldRejectAndNotify() {
        // Given
        LoanDecisionDTO decision = TestDataBuilder.rejectionDecision().build();
        LoanRequest existingRequest = TestDataBuilder.defaultLoanRequest()
                .id(decision.getSolicitudId())
                .status(LoanRequest.LoanStatus.PENDING_REVIEW)
                .build();
        when(loanRequestRepository.findById(decision.getSolicitudId()))
                .thenReturn(Mono.just(existingRequest));
        when(loanRequestRepository.save(any(LoanRequest.class)))
                .thenAnswer(invocation -> {
                    LoanRequest updated = invocation.getArgument(0);
                    return Mono.just(updated);
                });
        when(notificationService.sendLoanDecisionNotification(any(LoanRequest.class), anyString()))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(useCase.processDecision(decision))
                .expectNextMatches(result -> 
                    result.getStatus() == LoanRequest.LoanStatus.REJECTED &&
                    result.getRejectionReason().equals(decision.getMotivo()) &&
                    result.getNotes().equals(decision.getMotivo())
                )
                .verifyComplete();

        verify(loanRequestRepository).findById(decision.getSolicitudId());
        verify(loanRequestRepository).save(any(LoanRequest.class));
        verify(notificationService).sendLoanDecisionNotification(any(LoanRequest.class), eq(decision.getAsesorEmail()));
    }

    @Test
    void processDecision_WithAsesorId_ShouldSetApprovedBy() {
        // Given
        LoanDecisionDTO decision = TestDataBuilder.approvalDecision().build();
        String asesorId = "asesor-123";
        LoanRequest existingRequest = TestDataBuilder.defaultLoanRequest()
                .id(decision.getSolicitudId())
                .status(LoanRequest.LoanStatus.PENDING_REVIEW)
                .build();

        when(loanRequestRepository.findById(decision.getSolicitudId()))
                .thenReturn(Mono.just(existingRequest));
        when(loanRequestRepository.save(any(LoanRequest.class)))
                .thenAnswer(invocation -> {
                    LoanRequest savedLoan = invocation.getArgument(0);
                    if (!asesorId.equals(savedLoan.getApprovedBy())) {
                        throw new RuntimeException("AsesorId not set correctly");
                    }
                    return Mono.just(savedLoan);
                });
        when(notificationService.sendLoanDecisionNotification(any(LoanRequest.class), anyString()))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(useCase.processDecision(decision, asesorId))
                .expectNextMatches(result -> result.getApprovedBy().equals(asesorId))
                .verifyComplete();
    }

    // ==========================================
    // VALIDATION TESTS
    // ==========================================

    @Test
    void processDecision_WithNullDecision_ShouldFail() {
        // When & Then  
        StepVerifier.create(useCase.processDecision(null))
                .expectError()
                .verify();

        verify(loanRequestRepository, never()).findById(anyLong());
    }

    @Test
    void processDecision_WithNullSolicitudId_ShouldFail() {
        // Given
        LoanDecisionDTO decision = TestDataBuilder.approvalDecision()
                .solicitudId(null)
                .build();

        // When & Then
        StepVerifier.create(useCase.processDecision(decision))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void processDecision_WithNullDecisionType_ShouldFail() {
        // Given
        LoanDecisionDTO decision = TestDataBuilder.approvalDecision()
                .decision(null)
                .build();

        // When & Then
        StepVerifier.create(useCase.processDecision(decision))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void processDecision_WithInvalidDecisionType_ShouldFail() {
        // Given
        LoanDecisionDTO decision = TestDataBuilder.approvalDecision()
                .decision(LoanRequest.LoanStatus.PENDING_REVIEW) // Invalid decision type
                .build();

        // When & Then
        StepVerifier.create(useCase.processDecision(decision))
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException &&
                    throwable.getMessage().contains("decisión debe ser APPROVED o REJECTED")
                )
                .verify();
    }

    @Test
    void processDecision_WithNullAsesorEmail_ShouldFail() {
        // Given
        LoanDecisionDTO decision = TestDataBuilder.approvalDecision()
                .asesorEmail(null)
                .build();

        // When & Then
        StepVerifier.create(useCase.processDecision(decision))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void processDecision_WithEmptyAsesorEmail_ShouldFail() {
        // Given
        LoanDecisionDTO decision = TestDataBuilder.approvalDecision()
                .asesorEmail("   ")
                .build();

        // When & Then
        StepVerifier.create(useCase.processDecision(decision))
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException &&
                    throwable.getMessage().contains("email del asesor")
                )
                .verify();
    }

    // ==========================================
    // APPROVAL VALIDATION TESTS
    // ==========================================

    @Test
    void processDecision_ApprovalWithNullAmount_ShouldFail() {
        // Given
        LoanDecisionDTO decision = TestDataBuilder.approvalDecision()
                .montoAprobado(null)
                .build();

        // When & Then
        StepVerifier.create(useCase.processDecision(decision))
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException &&
                    throwable.getMessage().contains("monto aprobado es requerido")
                )
                .verify();
    }

    @Test
    void processDecision_ApprovalWithZeroAmount_ShouldFail() {
        // Given
        LoanDecisionDTO decision = TestDataBuilder.approvalDecision()
                .montoAprobado(BigDecimal.ZERO)
                .build();

        // When & Then
        StepVerifier.create(useCase.processDecision(decision))
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException &&
                    throwable.getMessage().contains("monto aprobado es requerido")
                )
                .verify();
    }

    @Test
    void processDecision_ApprovalWithNullInterestRate_ShouldFail() {
        // Given
        LoanDecisionDTO decision = TestDataBuilder.approvalDecision()
                .tasaInteres(null)
                .build();

        // When & Then
        StepVerifier.create(useCase.processDecision(decision))
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException &&
                    throwable.getMessage().contains("tasa de interés es requerida")
                )
                .verify();
    }

    @Test
    void processDecision_ApprovalWithZeroInterestRate_ShouldFail() {
        // Given
        LoanDecisionDTO decision = TestDataBuilder.approvalDecision()
                .tasaInteres(BigDecimal.ZERO)
                .build();

        // When & Then
        StepVerifier.create(useCase.processDecision(decision))
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException &&
                    throwable.getMessage().contains("tasa de interés es requerida")
                )
                .verify();
    }

    @Test
    void processDecision_ApprovalWithNullTerm_ShouldFail() {
        // Given
        LoanDecisionDTO decision = TestDataBuilder.approvalDecision()
                .plazoAprobado(null)
                .build();

        // When & Then
        StepVerifier.create(useCase.processDecision(decision))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void processDecision_ApprovalWithZeroTerm_ShouldFail() {
        // Given
        LoanDecisionDTO decision = TestDataBuilder.approvalDecision()
                .plazoAprobado(0)
                .build();

        // When & Then
        StepVerifier.create(useCase.processDecision(decision))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    // ==========================================
    // REJECTION VALIDATION TESTS
    // ==========================================

    @Test
    void processDecision_RejectionWithNullReason_ShouldFail() {
        // Given
        LoanDecisionDTO decision = TestDataBuilder.rejectionDecision()
                .motivo(null)
                .build();

        // When & Then
        StepVerifier.create(useCase.processDecision(decision))
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException &&
                    throwable.getMessage().contains("motivo de rechazo es requerido")
                )
                .verify();
    }

    @Test
    void processDecision_RejectionWithEmptyReason_ShouldFail() {
        // Given
        LoanDecisionDTO decision = TestDataBuilder.rejectionDecision()
                .motivo("   ")
                .build();

        // When & Then
        StepVerifier.create(useCase.processDecision(decision))
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException &&
                    throwable.getMessage().contains("motivo de rechazo es requerido")
                )
                .verify();
    }

    // ==========================================
    // LOAN REQUEST STATE VALIDATION TESTS
    // ==========================================

    @Test
    void processDecision_WithNonexistentLoan_ShouldFail() {
        // Given
        LoanDecisionDTO decision = TestDataBuilder.approvalDecision().build();

        when(loanRequestRepository.findById(decision.getSolicitudId()))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(useCase.processDecision(decision))
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(loanRequestRepository).findById(decision.getSolicitudId());
        verify(loanRequestRepository, never()).save(any(LoanRequest.class));
    }

    @ParameterizedTest
    @EnumSource(value = LoanRequest.LoanStatus.class, names = {"APPROVED", "REJECTED", "CANCELLED"})
    void processDecision_WithInvalidCurrentStatus_ShouldFail(LoanRequest.LoanStatus currentStatus) {
        // Given
        LoanDecisionDTO decision = TestDataBuilder.approvalDecision().build();
        LoanRequest existingRequest = TestDataBuilder.defaultLoanRequest()
                .id(decision.getSolicitudId())
                .status(currentStatus)
                .build();

        when(loanRequestRepository.findById(decision.getSolicitudId()))
                .thenReturn(Mono.just(existingRequest));

        // When & Then
        StepVerifier.create(useCase.processDecision(decision))
                .expectErrorMatches(throwable -> throwable instanceof IllegalStateException)
                .verify();

        verify(loanRequestRepository).findById(decision.getSolicitudId());
        verify(loanRequestRepository, never()).save(any(LoanRequest.class));
    }

    @ParameterizedTest
    @EnumSource(value = LoanRequest.LoanStatus.class, names = {"PENDING_REVIEW", "MANUAL_REVIEW"})
    void processDecision_WithValidCurrentStatus_ShouldWork(LoanRequest.LoanStatus currentStatus) {
        // Given
        LoanDecisionDTO decision = TestDataBuilder.approvalDecision().build();
        LoanRequest existingRequest = TestDataBuilder.defaultLoanRequest()
                .id(decision.getSolicitudId())
                .status(currentStatus)
                .build();

        when(loanRequestRepository.findById(decision.getSolicitudId()))
                .thenReturn(Mono.just(existingRequest));
        when(loanRequestRepository.save(any(LoanRequest.class)))
                .thenAnswer(invocation -> {
                    LoanRequest updated = invocation.getArgument(0);
                    return Mono.just(updated);
                });
        when(notificationService.sendLoanDecisionNotification(any(LoanRequest.class), anyString()))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(useCase.processDecision(decision))
                .expectNextMatches(result -> result.getStatus() == LoanRequest.LoanStatus.APPROVED)
                .verifyComplete();
    }

    // ==========================================
    // MONTHLY PAYMENT CALCULATION TESTS
    // ==========================================

    @Test
    void processDecision_ApprovalWithZeroInterestRate_ShouldCalculatePaymentWithoutInterest() {
        // Given
        LoanDecisionDTO decision = TestDataBuilder.approvalDecision()
                .montoAprobado(new BigDecimal("12000000"))
                .tasaInteres(BigDecimal.ZERO)
                .plazoAprobado(12)
                .build();
        LoanRequest existingRequest = TestDataBuilder.defaultLoanRequest()
                .id(decision.getSolicitudId())
                .status(LoanRequest.LoanStatus.PENDING_REVIEW)
                .build();

        when(loanRequestRepository.findById(decision.getSolicitudId()))
                .thenReturn(Mono.just(existingRequest));
        when(loanRequestRepository.save(any(LoanRequest.class)))
                .thenAnswer(invocation -> {
                    LoanRequest updated = invocation.getArgument(0);
                    return Mono.just(updated);
                });
        when(notificationService.sendLoanDecisionNotification(any(LoanRequest.class), anyString()))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(useCase.processDecision(decision))
                .expectNextMatches(result -> 
                    result.getMonthlyPayment() != null &&
                    result.getMonthlyPayment().compareTo(BigDecimal.ZERO) > 0
                )
                .verifyComplete();
    }

    @Test
    void processDecision_ApprovalWithInterestRate_ShouldCalculatePaymentWithInterest() {
        // Given
        LoanDecisionDTO decision = TestDataBuilder.approvalDecision()
                .montoAprobado(new BigDecimal("10000000"))
                .tasaInteres(new BigDecimal("12.0")) // 12% annual
                .plazoAprobado(36)
                .build();
        LoanRequest existingRequest = TestDataBuilder.defaultLoanRequest()
                .id(decision.getSolicitudId())
                .status(LoanRequest.LoanStatus.PENDING_REVIEW)
                .build();

        when(loanRequestRepository.findById(decision.getSolicitudId()))
                .thenReturn(Mono.just(existingRequest));
        when(loanRequestRepository.save(any(LoanRequest.class)))
                .thenAnswer(invocation -> {
                    LoanRequest updated = invocation.getArgument(0);
                    return Mono.just(updated);
                });
        when(notificationService.sendLoanDecisionNotification(any(LoanRequest.class), anyString()))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(useCase.processDecision(decision))
                .expectNextMatches(result -> 
                    result.getMonthlyPayment() != null &&
                    result.getMonthlyPayment().compareTo(BigDecimal.ZERO) > 0
                )
                .verifyComplete();
    }

    // ==========================================
    // NOTIFICATION ERROR TESTS
    // ==========================================

    @Test
    void processDecision_WithNotificationError_ShouldStillCompleteSuccessfully() {
        // Given
        LoanDecisionDTO decision = TestDataBuilder.approvalDecision().build();
        LoanRequest existingRequest = TestDataBuilder.defaultLoanRequest()
                .id(decision.getSolicitudId())
                .status(LoanRequest.LoanStatus.PENDING_REVIEW)
                .build();

        when(loanRequestRepository.findById(decision.getSolicitudId()))
                .thenReturn(Mono.just(existingRequest));
        when(loanRequestRepository.save(any(LoanRequest.class)))
                .thenAnswer(invocation -> {
                    LoanRequest updated = invocation.getArgument(0);
                    return Mono.just(updated);
                });
        when(notificationService.sendLoanDecisionNotification(any(LoanRequest.class), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Notification service error")));

        // When & Then
        StepVerifier.create(useCase.processDecision(decision))
                .expectError(RuntimeException.class)
                .verify();

        verify(notificationService).sendLoanDecisionNotification(any(LoanRequest.class), anyString());
    }

    // ==========================================
    // DATABASE ERROR TESTS
    // ==========================================

    @Test
    void processDecision_WithRepositoryErrorOnFind_ShouldPropagateError() {
        // Given
        LoanDecisionDTO decision = TestDataBuilder.approvalDecision().build();

        when(loanRequestRepository.findById(decision.getSolicitudId()))
                .thenReturn(Mono.error(new RuntimeException("Database connection error")));

        // When & Then
        StepVerifier.create(useCase.processDecision(decision))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException &&
                    throwable.getMessage().contains("Database connection error")
                )
                .verify();
    }

    @Test
    void processDecision_WithRepositoryErrorOnSave_ShouldPropagateError() {
        // Given
        LoanDecisionDTO decision = TestDataBuilder.approvalDecision().build();
        LoanRequest existingRequest = TestDataBuilder.defaultLoanRequest()
                .id(decision.getSolicitudId())
                .status(LoanRequest.LoanStatus.PENDING_REVIEW)
                .build();

        when(loanRequestRepository.findById(decision.getSolicitudId()))
                .thenReturn(Mono.just(existingRequest));
        when(loanRequestRepository.save(any(LoanRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("Save error")));

        // When & Then
        StepVerifier.create(useCase.processDecision(decision))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException &&
                    throwable.getMessage().contains("Save error")
                )
                .verify();
    }
}
