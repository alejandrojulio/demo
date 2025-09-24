package co.com.pragma.usecase.loan;

import co.com.pragma.model.TestDataBuilder;
import co.com.pragma.model.loan.LoanRequest;
import co.com.pragma.model.loan.gateways.LoanApplicationLogger;
import co.com.pragma.model.loan.gateways.LoanRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests para UpdateLoanStatusUseCase
 * Cubre todos los escenarios de actualización de estado desde Lambda
 */
@ExtendWith(MockitoExtension.class)
class UpdateLoanStatusUseCaseTest {

    @Mock
    private LoanRequestRepository loanRequestRepository;
    
    @Mock
    private LoanApplicationLogger logger;

    private UpdateLoanStatusUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateLoanStatusUseCase(loanRequestRepository, logger);
    }

    // ==========================================
    // SUCCESSFUL UPDATE TESTS
    // ==========================================

    @Test
    void updateLoanStatus_FromPendingToApproved_ShouldUpdateSuccessfully() {
        // Given
        Long loanRequestId = 1L;
        String newStatus = "APPROVED";
        String reason = "Automatic approval";
        
        LoanRequest existingLoan = TestDataBuilder.defaultLoanRequest()
                .id(loanRequestId)
                .status(LoanRequest.LoanStatus.PENDING_REVIEW)
                .build();
        
        LoanRequest updatedLoan = existingLoan.toBuilder()
                .status(LoanRequest.LoanStatus.APPROVED)
                .build();

        when(loanRequestRepository.findById(loanRequestId))
                .thenReturn(Mono.just(existingLoan));
        when(loanRequestRepository.update(any(LoanRequest.class)))
                .thenReturn(Mono.just(updatedLoan));

        // When & Then
        StepVerifier.create(useCase.updateLoanStatus(loanRequestId, newStatus, reason))
                .expectNextMatches(result -> 
                    result.getStatus() == LoanRequest.LoanStatus.APPROVED &&
                    result.getId().equals(loanRequestId)
                )
                .verifyComplete();

        verify(loanRequestRepository).findById(loanRequestId);
        verify(loanRequestRepository).update(any(LoanRequest.class));
    }

    @Test
    void updateLoanStatus_FromPendingToRejected_ShouldUpdateWithReason() {
        // Given
        Long loanRequestId = 1L;
        String newStatus = "REJECTED";
        String reason = "Insufficient income";
        
        LoanRequest existingLoan = TestDataBuilder.defaultLoanRequest()
                .id(loanRequestId)
                .status(LoanRequest.LoanStatus.PENDING_REVIEW)
                .build();

        when(loanRequestRepository.findById(loanRequestId))
                .thenReturn(Mono.just(existingLoan));
        when(loanRequestRepository.update(any(LoanRequest.class)))
                .thenAnswer(invocation -> {
                    LoanRequest updated = invocation.getArgument(0);
                    if (!reason.equals(updated.getRejectionReason())) {
                        throw new RuntimeException("Rejection reason not set");
                    }
                    return Mono.just(updated);
                });

        // When & Then
        StepVerifier.create(useCase.updateLoanStatus(loanRequestId, newStatus, reason))
                .expectNextMatches(result -> 
                    result.getStatus() == LoanRequest.LoanStatus.REJECTED &&
                    result.getRejectionReason().equals(reason)
                )
                .verifyComplete();
    }

    @Test
    void updateLoanStatus_FromPendingToManualReview_ShouldUpdateSuccessfully() {
        // Given
        Long loanRequestId = 1L;
        String newStatus = "MANUAL_REVIEW";
        String reason = "Requires manual verification";
        
        LoanRequest existingLoan = TestDataBuilder.defaultLoanRequest()
                .id(loanRequestId)
                .status(LoanRequest.LoanStatus.PENDING_REVIEW)
                .build();

        when(loanRequestRepository.findById(loanRequestId))
                .thenReturn(Mono.just(existingLoan));
        when(loanRequestRepository.update(any(LoanRequest.class)))
                .thenReturn(Mono.just(existingLoan.toBuilder().status(LoanRequest.LoanStatus.MANUAL_REVIEW).build()));

        // When & Then
        StepVerifier.create(useCase.updateLoanStatus(loanRequestId, newStatus, reason))
                .expectNextMatches(result -> result.getStatus() == LoanRequest.LoanStatus.MANUAL_REVIEW)
                .verifyComplete();
    }

    @Test
    void updateLoanStatus_FromManualReviewToApproved_ShouldUpdateSuccessfully() {
        // Given
        Long loanRequestId = 1L;
        String newStatus = "APPROVED";
        String reason = "Manual approval";
        
        LoanRequest existingLoan = TestDataBuilder.manualReviewLoanRequest()
                .id(loanRequestId)
                .build();

        when(loanRequestRepository.findById(loanRequestId))
                .thenReturn(Mono.just(existingLoan));
        when(loanRequestRepository.update(any(LoanRequest.class)))
                .thenReturn(Mono.just(existingLoan.toBuilder().status(LoanRequest.LoanStatus.APPROVED).build()));

        // When & Then
        StepVerifier.create(useCase.updateLoanStatus(loanRequestId, newStatus, reason))
                .expectNextMatches(result -> result.getStatus() == LoanRequest.LoanStatus.APPROVED)
                .verifyComplete();
    }

    @Test
    void updateLoanStatus_FromApprovedToCancelled_ShouldUpdateSuccessfully() {
        // Given
        Long loanRequestId = 1L;
        String newStatus = "CANCELLED";
        String reason = "Client request";
        
        LoanRequest existingLoan = TestDataBuilder.approvedLoanRequest()
                .id(loanRequestId)
                .build();

        when(loanRequestRepository.findById(loanRequestId))
                .thenReturn(Mono.just(existingLoan));
        when(loanRequestRepository.update(any(LoanRequest.class)))
                .thenReturn(Mono.just(existingLoan.toBuilder().status(LoanRequest.LoanStatus.CANCELLED).build()));

        // When & Then
        StepVerifier.create(useCase.updateLoanStatus(loanRequestId, newStatus, reason))
                .expectNextMatches(result -> result.getStatus() == LoanRequest.LoanStatus.CANCELLED)
                .verifyComplete();
    }

    // ==========================================
    // STATUS MAPPING TESTS
    // ==========================================

    @ParameterizedTest
    @ValueSource(strings = {"APPROVED", "approved", "Approved"})
    void updateLoanStatus_WithDifferentCaseApproved_ShouldMapCorrectly(String statusVariation) {
        // Given
        Long loanRequestId = 1L;
        String reason = "Test";
        
        LoanRequest existingLoan = TestDataBuilder.defaultLoanRequest()
                .id(loanRequestId)
                .status(LoanRequest.LoanStatus.PENDING_REVIEW)
                .build();

        when(loanRequestRepository.findById(loanRequestId))
                .thenReturn(Mono.just(existingLoan));
        when(loanRequestRepository.update(any(LoanRequest.class)))
                .thenReturn(Mono.just(existingLoan.toBuilder().status(LoanRequest.LoanStatus.APPROVED).build()));

        // When & Then
        StepVerifier.create(useCase.updateLoanStatus(loanRequestId, statusVariation, reason))
                .expectNextMatches(result -> result.getStatus() == LoanRequest.LoanStatus.APPROVED)
                .verifyComplete();
    }

    @ParameterizedTest
    @ValueSource(strings = {"REJECTED", "rejected", "Rejected"})
    void updateLoanStatus_WithDifferentCaseRejected_ShouldMapCorrectly(String statusVariation) {
        // Given
        Long loanRequestId = 1L;
        String reason = "Test rejection";
        
        LoanRequest existingLoan = TestDataBuilder.defaultLoanRequest()
                .id(loanRequestId)
                .status(LoanRequest.LoanStatus.PENDING_REVIEW)
                .build();

        when(loanRequestRepository.findById(loanRequestId))
                .thenReturn(Mono.just(existingLoan));
        when(loanRequestRepository.update(any(LoanRequest.class)))
                .thenReturn(Mono.just(existingLoan.toBuilder().status(LoanRequest.LoanStatus.REJECTED).build()));

        // When & Then
        StepVerifier.create(useCase.updateLoanStatus(loanRequestId, statusVariation, reason))
                .expectNextMatches(result -> result.getStatus() == LoanRequest.LoanStatus.REJECTED)
                .verifyComplete();
    }

    @Test
    void updateLoanStatus_WithUnknownStatus_ShouldDefaultToPendingReview() {
        // Given
        Long loanRequestId = 1L;
        String newStatus = "UNKNOWN_STATUS";
        String reason = "Test";
        
        LoanRequest existingLoan = TestDataBuilder.defaultLoanRequest()
                .id(loanRequestId)
                .status(LoanRequest.LoanStatus.PENDING_REVIEW)
                .build();

        when(loanRequestRepository.findById(loanRequestId))
                .thenReturn(Mono.just(existingLoan));
        when(loanRequestRepository.update(any(LoanRequest.class)))
                .thenReturn(Mono.just(existingLoan));

        // When & Then
        StepVerifier.create(useCase.updateLoanStatus(loanRequestId, newStatus, reason))
                .expectNextMatches(result -> result.getStatus() == LoanRequest.LoanStatus.PENDING_REVIEW)
                .verifyComplete();
    }

    // ==========================================
    // VALIDATION TESTS
    // ==========================================

    @Test
    void updateLoanStatus_WithNonexistentLoan_ShouldFail() {
        // Given
        Long loanRequestId = 999L;
        String newStatus = "APPROVED";
        String reason = "Test";

        when(loanRequestRepository.findById(loanRequestId))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(useCase.updateLoanStatus(loanRequestId, newStatus, reason))
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(loanRequestRepository).findById(loanRequestId);
        verify(loanRequestRepository, never()).update(any(LoanRequest.class));
    }

    // ==========================================
    // STATUS CHANGE VALIDATION TESTS
    // ==========================================

    @Test
    void updateLoanStatus_FromRejectedToApproved_ShouldFail() {
        // Given
        Long loanRequestId = 1L;
        String newStatus = "APPROVED";
        String reason = "Test";
        
        LoanRequest existingLoan = TestDataBuilder.rejectedLoanRequest()
                .id(loanRequestId)
                .build();

        when(loanRequestRepository.findById(loanRequestId))
                .thenReturn(Mono.just(existingLoan));

        // When & Then
        StepVerifier.create(useCase.updateLoanStatus(loanRequestId, newStatus, reason))
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalStateException &&
                    throwable.getMessage().contains("Cambio de estado inválido")
                )
                .verify();

        verify(loanRequestRepository, never()).update(any(LoanRequest.class));
    }

    @Test
    void updateLoanStatus_FromCancelledToApproved_ShouldFail() {
        // Given
        Long loanRequestId = 1L;
        String newStatus = "APPROVED";
        String reason = "Test";
        
        LoanRequest existingLoan = TestDataBuilder.cancelledLoanRequest()
                .id(loanRequestId)
                .build();

        when(loanRequestRepository.findById(loanRequestId))
                .thenReturn(Mono.just(existingLoan));

        // When & Then
        StepVerifier.create(useCase.updateLoanStatus(loanRequestId, newStatus, reason))
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalStateException &&
                    throwable.getMessage().contains("Cambio de estado inválido")
                )
                .verify();

        verify(loanRequestRepository, never()).update(any(LoanRequest.class));
    }

    @Test
    void updateLoanStatus_FromApprovedToRejected_ShouldFail() {
        // Given
        Long loanRequestId = 1L;
        String newStatus = "REJECTED";
        String reason = "Test";
        
        LoanRequest existingLoan = TestDataBuilder.approvedLoanRequest()
                .id(loanRequestId)
                .build();

        when(loanRequestRepository.findById(loanRequestId))
                .thenReturn(Mono.just(existingLoan));

        // When & Then
        StepVerifier.create(useCase.updateLoanStatus(loanRequestId, newStatus, reason))
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalStateException &&
                    throwable.getMessage().contains("Cambio de estado inválido")
                )
                .verify();
    }

    // ==========================================
    // EDGE CASES TESTS
    // ==========================================

    @Test
    void updateLoanStatus_FromPendingToPending_ShouldAllowSameStatus() {
        // Given
        Long loanRequestId = 1L;
        String newStatus = "PENDING_REVIEW";
        String reason = "Reset status";
        
        LoanRequest existingLoan = TestDataBuilder.defaultLoanRequest()
                .id(loanRequestId)
                .status(LoanRequest.LoanStatus.PENDING_REVIEW)
                .build();

        when(loanRequestRepository.findById(loanRequestId))
                .thenReturn(Mono.just(existingLoan));
        when(loanRequestRepository.update(any(LoanRequest.class)))
                .thenReturn(Mono.just(existingLoan));

        // When & Then
        StepVerifier.create(useCase.updateLoanStatus(loanRequestId, newStatus, reason))
                .expectNextMatches(result -> result.getStatus() == LoanRequest.LoanStatus.PENDING_REVIEW)
                .verifyComplete();
    }

    @Test
    void updateLoanStatus_WithNullReason_ShouldWork() {
        // Given
        Long loanRequestId = 1L;
        String newStatus = "APPROVED";
        String reason = null;
        
        LoanRequest existingLoan = TestDataBuilder.defaultLoanRequest()
                .id(loanRequestId)
                .status(LoanRequest.LoanStatus.PENDING_REVIEW)
                .build();

        when(loanRequestRepository.findById(loanRequestId))
                .thenReturn(Mono.just(existingLoan));
        when(loanRequestRepository.update(any(LoanRequest.class)))
                .thenReturn(Mono.just(existingLoan.toBuilder().status(LoanRequest.LoanStatus.APPROVED).build()));

        // When & Then
        StepVerifier.create(useCase.updateLoanStatus(loanRequestId, newStatus, reason))
                .expectNextMatches(result -> result.getStatus() == LoanRequest.LoanStatus.APPROVED)
                .verifyComplete();
    }

    @Test
    void updateLoanStatus_WithEmptyReason_ShouldWork() {
        // Given
        Long loanRequestId = 1L;
        String newStatus = "APPROVED";
        String reason = "";
        
        LoanRequest existingLoan = TestDataBuilder.defaultLoanRequest()
                .id(loanRequestId)
                .status(LoanRequest.LoanStatus.PENDING_REVIEW)
                .build();

        when(loanRequestRepository.findById(loanRequestId))
                .thenReturn(Mono.just(existingLoan));
        when(loanRequestRepository.update(any(LoanRequest.class)))
                .thenReturn(Mono.just(existingLoan.toBuilder().status(LoanRequest.LoanStatus.APPROVED).build()));

        // When & Then
        StepVerifier.create(useCase.updateLoanStatus(loanRequestId, newStatus, reason))
                .expectNextMatches(result -> result.getStatus() == LoanRequest.LoanStatus.APPROVED)
                .verifyComplete();
    }

    // ==========================================
    // DATABASE ERROR TESTS
    // ==========================================

    @Test
    void updateLoanStatus_WithRepositoryErrorOnFind_ShouldPropagateError() {
        // Given
        Long loanRequestId = 1L;
        String newStatus = "APPROVED";
        String reason = "Test";

        when(loanRequestRepository.findById(loanRequestId))
                .thenReturn(Mono.error(new RuntimeException("Database connection error")));

        // When & Then
        StepVerifier.create(useCase.updateLoanStatus(loanRequestId, newStatus, reason))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException &&
                    throwable.getMessage().contains("Database connection error")
                )
                .verify();
    }

    @Test
    void updateLoanStatus_WithRepositoryErrorOnUpdate_ShouldPropagateError() {
        // Given
        Long loanRequestId = 1L;
        String newStatus = "APPROVED";
        String reason = "Test";
        
        LoanRequest existingLoan = TestDataBuilder.defaultLoanRequest()
                .id(loanRequestId)
                .status(LoanRequest.LoanStatus.PENDING_REVIEW)
                .build();

        when(loanRequestRepository.findById(loanRequestId))
                .thenReturn(Mono.just(existingLoan));
        when(loanRequestRepository.update(any(LoanRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("Update failed")));

        // When & Then
        StepVerifier.create(useCase.updateLoanStatus(loanRequestId, newStatus, reason))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException &&
                    throwable.getMessage().contains("Update failed")
                )
                .verify();
    }

    // ==========================================
    // TIMESTAMP UPDATE TESTS
    // ==========================================

    @Test
    void updateLoanStatus_ShouldUpdateTimestamp() {
        // Given
        Long loanRequestId = 1L;
        String newStatus = "APPROVED";
        String reason = "Test";
        
        LoanRequest existingLoan = TestDataBuilder.defaultLoanRequest()
                .id(loanRequestId)
                .status(LoanRequest.LoanStatus.PENDING_REVIEW)
                .build();

        when(loanRequestRepository.findById(loanRequestId))
                .thenReturn(Mono.just(existingLoan));
        when(loanRequestRepository.update(any(LoanRequest.class)))
                .thenAnswer(invocation -> {
                    LoanRequest updated = invocation.getArgument(0);
                    if (updated.getUpdatedAt() == null || 
                        !updated.getUpdatedAt().isAfter(existingLoan.getUpdatedAt())) {
                        throw new RuntimeException("UpdatedAt not properly set");
                    }
                    return Mono.just(updated);
                });

        // When & Then
        StepVerifier.create(useCase.updateLoanStatus(loanRequestId, newStatus, reason))
                .expectNextMatches(result -> result.getUpdatedAt() != null)
                .verifyComplete();
    }

    // ==========================================
    // COMPREHENSIVE STATUS TRANSITION TESTS
    // ==========================================

    @Test
    void updateLoanStatus_AllValidTransitionsFromPending_ShouldWork() {
        testStatusTransition(LoanRequest.LoanStatus.PENDING_REVIEW, "APPROVED", true);
        testStatusTransition(LoanRequest.LoanStatus.PENDING_REVIEW, "REJECTED", true);
        testStatusTransition(LoanRequest.LoanStatus.PENDING_REVIEW, "MANUAL_REVIEW", true);
        testStatusTransition(LoanRequest.LoanStatus.PENDING_REVIEW, "CANCELLED", true);
    }

    @Test
    void updateLoanStatus_AllValidTransitionsFromManualReview_ShouldWork() {
        testStatusTransition(LoanRequest.LoanStatus.MANUAL_REVIEW, "APPROVED", true);
        testStatusTransition(LoanRequest.LoanStatus.MANUAL_REVIEW, "REJECTED", true);
        testStatusTransition(LoanRequest.LoanStatus.MANUAL_REVIEW, "CANCELLED", true);
    }

    @Test
    void updateLoanStatus_ValidTransitionsFromApproved_ShouldWork() {
        testStatusTransition(LoanRequest.LoanStatus.APPROVED, "CANCELLED", true);
    }

    @Test
    void updateLoanStatus_InvalidTransitionsFromRejected_ShouldFail() {
        testStatusTransition(LoanRequest.LoanStatus.REJECTED, "APPROVED", false);
        testStatusTransition(LoanRequest.LoanStatus.REJECTED, "MANUAL_REVIEW", false);
    }

    @Test
    void updateLoanStatus_InvalidTransitionsFromCancelled_ShouldFail() {
        testStatusTransition(LoanRequest.LoanStatus.CANCELLED, "APPROVED", false);
        testStatusTransition(LoanRequest.LoanStatus.CANCELLED, "REJECTED", false);
        testStatusTransition(LoanRequest.LoanStatus.CANCELLED, "PENDING_REVIEW", false);
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private void testStatusTransition(LoanRequest.LoanStatus currentStatus, String newStatus, boolean shouldSucceed) {
        // Given
        Long loanRequestId = System.currentTimeMillis(); // Unique ID for each test
        String reason = "Test transition";
        
        LoanRequest existingLoan = TestDataBuilder.defaultLoanRequest()
                .id(loanRequestId)
                .status(currentStatus)
                .build();

        when(loanRequestRepository.findById(loanRequestId))
                .thenReturn(Mono.just(existingLoan));
        
        if (shouldSucceed) {
            when(loanRequestRepository.update(any(LoanRequest.class)))
                    .thenReturn(Mono.just(existingLoan));
            
            // When & Then
            StepVerifier.create(useCase.updateLoanStatus(loanRequestId, newStatus, reason))
                    .expectNextCount(1)
                    .verifyComplete();
        } else {
            // When & Then
            StepVerifier.create(useCase.updateLoanStatus(loanRequestId, newStatus, reason))
                    .expectError(IllegalStateException.class)
                    .verify();
        }

        // Reset mocks for next test
        reset(loanRequestRepository);
    }
}
