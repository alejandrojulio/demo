package co.com.pragma.usecase.notification;

import co.com.pragma.model.TestDataBuilder;
import co.com.pragma.model.loan.LoanRequest;
import co.com.pragma.model.loan.LoanRequestReviewDTO;
import co.com.pragma.model.loan.gateways.LoanApplicationLogger;
import co.com.pragma.model.loan.gateways.LoanRequestRepository;
import co.com.pragma.model.notification.NotificationMessage;
import co.com.pragma.model.notification.gateways.NotificationGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests para NotificationService
 * Cubre todos los escenarios de envío de notificaciones
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationGateway notificationGateway;
    
    @Mock
    private LoanApplicationLogger logger;
    
    @Mock
    private LoanRequestRepository loanRequestRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
            notificationGateway, 
            logger, 
            loanRequestRepository
        );
    }

    // ==========================================
    // SUCCESSFUL NOTIFICATION TESTS
    // ==========================================

    @Test
    void sendLoanDecisionNotification_WithApprovedLoan_ShouldSendApprovalNotification() {
        // Given
        LoanRequest approvedLoan = TestDataBuilder.approvedLoanRequest()
                .id(1L)
                .approvedAmount(new BigDecimal("10000000"))
                .interestRate(new BigDecimal("15.5"))
                .termInMonths(36)
                .monthlyPayment(new BigDecimal("350000"))
                .build();
        
        String asesorEmail = TestDataBuilder.SecurityContextBuilder.ADVISOR_EMAIL;
        
        LoanRequestReviewDTO clientInfo = TestDataBuilder.defaultReviewDTO()
                .email("cliente@test.com")
                .nombre("Juan Pérez")
                .build();

        when(loanRequestRepository.findByIdWithClientInfo(approvedLoan.getId()))
                .thenReturn(Mono.just(clientInfo));
        when(notificationGateway.sendNotification(any(NotificationMessage.class)))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(notificationService.sendLoanDecisionNotification(approvedLoan, asesorEmail))
                .verifyComplete();

        // Verify notification content
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationGateway).sendNotification(messageCaptor.capture());
        
        NotificationMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getEventType()).isEqualTo("LOAN_APPROVED");
        assertThat(sentMessage.getSolicitudId()).isEqualTo(1L);
        assertThat(sentMessage.getClientEmail()).isEqualTo(clientInfo.getEmail());
        assertThat(sentMessage.getClientName()).isEqualTo(clientInfo.getNombre());
        assertThat(sentMessage.getDecision()).isEqualTo("APPROVED");
        assertThat(sentMessage.getAsesorEmail()).isEqualTo(asesorEmail);
        assertThat(sentMessage.getMontoAprobado()).contains("10,000,000");
        assertThat(sentMessage.getTasaInteres()).isEqualTo("15.5%");
        assertThat(sentMessage.getPlazoAprobado()).isEqualTo(36);
        assertThat(sentMessage.getPagoMensual()).contains("350,000");
        assertThat(sentMessage.getMessageId()).isNotNull();
        assertThat(sentMessage.getTimestamp()).isNotNull();
    }

    @Test
    void sendLoanDecisionNotification_WithRejectedLoan_ShouldSendRejectionNotification() {
        // Given
        LoanRequest rejectedLoan = TestDataBuilder.rejectedLoanRequest()
                .id(2L)
                .rejectionReason("Ingresos insuficientes para el monto solicitado")
                .build();
        
        String asesorEmail = TestDataBuilder.SecurityContextBuilder.ADVISOR_EMAIL;
        
        LoanRequestReviewDTO clientInfo = TestDataBuilder.defaultReviewDTO()
                .email("cliente@test.com")
                .nombre("María García")
                .build();

        when(loanRequestRepository.findByIdWithClientInfo(rejectedLoan.getId()))
                .thenReturn(Mono.just(clientInfo));
        when(notificationGateway.sendNotification(any(NotificationMessage.class)))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(notificationService.sendLoanDecisionNotification(rejectedLoan, asesorEmail))
                .verifyComplete();

        // Verify notification content
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationGateway).sendNotification(messageCaptor.capture());
        
        NotificationMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getEventType()).isEqualTo("LOAN_REJECTED");
        assertThat(sentMessage.getSolicitudId()).isEqualTo(2L);
        assertThat(sentMessage.getClientEmail()).isEqualTo(clientInfo.getEmail());
        assertThat(sentMessage.getClientName()).isEqualTo(clientInfo.getNombre());
        assertThat(sentMessage.getDecision()).isEqualTo("REJECTED");
        assertThat(sentMessage.getAsesorEmail()).isEqualTo(asesorEmail);
        assertThat(sentMessage.getReason()).isEqualTo(rejectedLoan.getRejectionReason());
        assertThat(sentMessage.getMontoAprobado()).isNull();
        assertThat(sentMessage.getTasaInteres()).isNull();
        assertThat(sentMessage.getPlazoAprobado()).isNull();
        assertThat(sentMessage.getPagoMensual()).isNull();
    }

    @Test
    void sendLoanDecisionNotification_WithApprovedLoanAndNotes_ShouldIncludeNotes() {
        // Given
        LoanRequest approvedLoan = TestDataBuilder.approvedLoanRequest()
                .id(3L)
                .notes("Aprobación condicionada a verificación de ingresos")
                .build();
        
        String asesorEmail = TestDataBuilder.SecurityContextBuilder.ADVISOR_EMAIL;
        
        LoanRequestReviewDTO clientInfo = TestDataBuilder.defaultReviewDTO()
                .email("cliente@test.com")
                .nombre("Carlos López")
                .build();

        when(loanRequestRepository.findByIdWithClientInfo(approvedLoan.getId()))
                .thenReturn(Mono.just(clientInfo));
        when(notificationGateway.sendNotification(any(NotificationMessage.class)))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(notificationService.sendLoanDecisionNotification(approvedLoan, asesorEmail))
                .verifyComplete();

        // Verify notes are included
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationGateway).sendNotification(messageCaptor.capture());
        
        NotificationMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getReason()).isEqualTo(approvedLoan.getNotes());
    }

    @Test
    void sendLoanDecisionNotification_WithRejectedLoanAndNullReason_ShouldUseDefaultReason() {
        // Given
        LoanRequest rejectedLoan = TestDataBuilder.rejectedLoanRequest()
                .id(4L)
                .rejectionReason(null)
                .build();
        
        String asesorEmail = TestDataBuilder.SecurityContextBuilder.ADVISOR_EMAIL;
        
        LoanRequestReviewDTO clientInfo = TestDataBuilder.defaultReviewDTO()
                .email("cliente@test.com")
                .nombre("Ana Rodríguez")
                .build();

        when(loanRequestRepository.findByIdWithClientInfo(rejectedLoan.getId()))
                .thenReturn(Mono.just(clientInfo));
        when(notificationGateway.sendNotification(any(NotificationMessage.class)))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(notificationService.sendLoanDecisionNotification(rejectedLoan, asesorEmail))
                .verifyComplete();

        // Verify default reason is used
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationGateway).sendNotification(messageCaptor.capture());
        
        NotificationMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getReason()).isEqualTo("No especificado");
    }

    // ==========================================
    // ERROR HANDLING TESTS
    // ==========================================

    @Test
    void sendLoanDecisionNotification_WithClientInfoError_ShouldPropagateError() {
        // Given
        LoanRequest approvedLoan = TestDataBuilder.approvedLoanRequest().id(1L).build();
        String asesorEmail = TestDataBuilder.SecurityContextBuilder.ADVISOR_EMAIL;

        when(loanRequestRepository.findByIdWithClientInfo(approvedLoan.getId()))
                .thenReturn(Mono.error(new RuntimeException("Client info not found")));

        // When & Then
        StepVerifier.create(notificationService.sendLoanDecisionNotification(approvedLoan, asesorEmail))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException &&
                    throwable.getMessage().contains("Client info not found")
                )
                .verify();

        verify(notificationGateway, never()).sendNotification(any(NotificationMessage.class));
    }

    @Test
    void sendLoanDecisionNotification_WithNotificationGatewayError_ShouldContinueGracefully() {
        // Given
        LoanRequest approvedLoan = TestDataBuilder.approvedLoanRequest().id(1L).build();
        String asesorEmail = TestDataBuilder.SecurityContextBuilder.ADVISOR_EMAIL;
        
        LoanRequestReviewDTO clientInfo = TestDataBuilder.defaultReviewDTO()
                .email("cliente@test.com")
                .nombre("Test User")
                .build();

        when(loanRequestRepository.findByIdWithClientInfo(approvedLoan.getId()))
                .thenReturn(Mono.just(clientInfo));
        when(notificationGateway.sendNotification(any(NotificationMessage.class)))
                .thenReturn(Mono.error(new RuntimeException("Notification service unavailable")));

        // When & Then
        StepVerifier.create(notificationService.sendLoanDecisionNotification(approvedLoan, asesorEmail))
                .verifyComplete(); // Should complete gracefully despite error

        verify(notificationGateway).sendNotification(any(NotificationMessage.class));
    }

    @Test
    void sendLoanDecisionNotification_WithMessageCreationError_ShouldPropagateError() {
        // Given
        LoanRequest approvedLoan = TestDataBuilder.approvedLoanRequest().id(1L).build();
        String asesorEmail = TestDataBuilder.SecurityContextBuilder.ADVISOR_EMAIL;
        
        LoanRequestReviewDTO clientInfo = TestDataBuilder.defaultReviewDTO()
                .email("cliente@test.com")
                .nombre("Test User")
                .build();

        when(loanRequestRepository.findByIdWithClientInfo(approvedLoan.getId()))
                .thenReturn(Mono.just(clientInfo));

        // Simulate an error during message creation by passing invalid data
        LoanRequest invalidLoan = approvedLoan.toBuilder()
                .status(null) // This should cause an error during message creation
                .build();

        // When & Then
        StepVerifier.create(notificationService.sendLoanDecisionNotification(invalidLoan, asesorEmail))
                .expectError()
                .verify();
    }

    // ==========================================
    // CURRENCY FORMATTING TESTS
    // ==========================================

    @Test
    void sendLoanDecisionNotification_WithDifferentAmounts_ShouldFormatCorrectly() {
        testCurrencyFormatting(new BigDecimal("1000000"), "1,000,000");
        testCurrencyFormatting(new BigDecimal("500000"), "500,000");
        testCurrencyFormatting(new BigDecimal("25000000"), "25,000,000");
        testCurrencyFormatting(new BigDecimal("100000.50"), "100,000.50");
    }

    @Test
    void sendLoanDecisionNotification_WithNullAmounts_ShouldHandleGracefully() {
        // Given
        LoanRequest approvedLoan = TestDataBuilder.approvedLoanRequest()
                .id(1L)
                .approvedAmount(null)
                .monthlyPayment(null)
                .build();
        
        String asesorEmail = TestDataBuilder.SecurityContextBuilder.ADVISOR_EMAIL;
        
        LoanRequestReviewDTO clientInfo = TestDataBuilder.defaultReviewDTO()
                .email("cliente@test.com")
                .nombre("Test User")
                .build();

        when(loanRequestRepository.findByIdWithClientInfo(approvedLoan.getId()))
                .thenReturn(Mono.just(clientInfo));
        when(notificationGateway.sendNotification(any(NotificationMessage.class)))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(notificationService.sendLoanDecisionNotification(approvedLoan, asesorEmail))
                .verifyComplete();

        // Verify N/A values are used
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationGateway).sendNotification(messageCaptor.capture());
        
        NotificationMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getMontoAprobado()).isEqualTo("N/A");
        assertThat(sentMessage.getPagoMensual()).isEqualTo("N/A");
    }

    // ==========================================
    // PERCENTAGE FORMATTING TESTS
    // ==========================================

    @Test
    void sendLoanDecisionNotification_WithDifferentInterestRates_ShouldFormatCorrectly() {
        testPercentageFormatting(new BigDecimal("15.5"), "15.5%");
        testPercentageFormatting(new BigDecimal("12.0"), "12.0%");
        testPercentageFormatting(new BigDecimal("18.75"), "18.75%");
        testPercentageFormatting(new BigDecimal("9"), "9%");
    }

    @Test
    void sendLoanDecisionNotification_WithNullInterestRate_ShouldHandleGracefully() {
        // Given
        LoanRequest approvedLoan = TestDataBuilder.approvedLoanRequest()
                .id(1L)
                .interestRate(null)
                .build();
        
        String asesorEmail = TestDataBuilder.SecurityContextBuilder.ADVISOR_EMAIL;
        
        LoanRequestReviewDTO clientInfo = TestDataBuilder.defaultReviewDTO()
                .email("cliente@test.com")
                .nombre("Test User")
                .build();

        when(loanRequestRepository.findByIdWithClientInfo(approvedLoan.getId()))
                .thenReturn(Mono.just(clientInfo));
        when(notificationGateway.sendNotification(any(NotificationMessage.class)))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(notificationService.sendLoanDecisionNotification(approvedLoan, asesorEmail))
                .verifyComplete();

        // Verify N/A value is used
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationGateway).sendNotification(messageCaptor.capture());
        
        NotificationMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getTasaInteres()).isEqualTo("N/A");
    }

    // ==========================================
    // DIFFERENT LOAN STATUSES TESTS
    // ==========================================

    @ParameterizedTest
    @EnumSource(value = LoanRequest.LoanStatus.class, names = {"APPROVED"})
    void sendLoanDecisionNotification_WithApprovedStatus_ShouldCreateApprovalMessage(LoanRequest.LoanStatus status) {
        // Given
        LoanRequest loan = TestDataBuilder.defaultLoanRequest()
                .id(1L)
                .status(status)
                .approvedAmount(new BigDecimal("5000000"))
                .interestRate(new BigDecimal("14.0"))
                .termInMonths(24)
                .monthlyPayment(new BigDecimal("250000"))
                .build();
        
        String asesorEmail = TestDataBuilder.SecurityContextBuilder.ADVISOR_EMAIL;
        
        LoanRequestReviewDTO clientInfo = TestDataBuilder.defaultReviewDTO()
                .email("cliente@test.com")
                .nombre("Test User")
                .build();

        when(loanRequestRepository.findByIdWithClientInfo(loan.getId()))
                .thenReturn(Mono.just(clientInfo));
        when(notificationGateway.sendNotification(any(NotificationMessage.class)))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(notificationService.sendLoanDecisionNotification(loan, asesorEmail))
                .verifyComplete();

        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationGateway).sendNotification(messageCaptor.capture());
        
        NotificationMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getEventType()).isEqualTo("LOAN_APPROVED");
        assertThat(sentMessage.getDecision()).isEqualTo(status.toString());
    }

    @ParameterizedTest
    @EnumSource(value = LoanRequest.LoanStatus.class, names = {"REJECTED"})
    void sendLoanDecisionNotification_WithRejectedStatus_ShouldCreateRejectionMessage(LoanRequest.LoanStatus status) {
        // Given
        LoanRequest loan = TestDataBuilder.defaultLoanRequest()
                .id(1L)
                .status(status)
                .rejectionReason("Test rejection reason")
                .build();
        
        String asesorEmail = TestDataBuilder.SecurityContextBuilder.ADVISOR_EMAIL;
        
        LoanRequestReviewDTO clientInfo = TestDataBuilder.defaultReviewDTO()
                .email("cliente@test.com")
                .nombre("Test User")
                .build();

        when(loanRequestRepository.findByIdWithClientInfo(loan.getId()))
                .thenReturn(Mono.just(clientInfo));
        when(notificationGateway.sendNotification(any(NotificationMessage.class)))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(notificationService.sendLoanDecisionNotification(loan, asesorEmail))
                .verifyComplete();

        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationGateway).sendNotification(messageCaptor.capture());
        
        NotificationMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getEventType()).isEqualTo("LOAN_REJECTED");
        assertThat(sentMessage.getDecision()).isEqualTo(status.toString());
    }

    // ==========================================
    // INTEGRATION TESTS
    // ==========================================

    @Test
    void sendLoanDecisionNotification_EndToEndFlow_ShouldWorkCorrectly() {
        // Given
        LoanRequest approvedLoan = TestDataBuilder.approvedLoanRequest()
                .id(1L)
                .approvedAmount(new BigDecimal("15000000"))
                .interestRate(new BigDecimal("16.2"))
                .termInMonths(48)
                .monthlyPayment(new BigDecimal("425000"))
                .notes("Aprobación especial")
                .build();
        
        String asesorEmail = "asesor.especial@crediya.com";
        
        LoanRequestReviewDTO clientInfo = LoanRequestReviewDTO.builder()
                .email("cliente.vip@empresa.com")
                .nombre("Cliente VIP Premium")
                .documentoCliente("12345678")
                .build();

        when(loanRequestRepository.findByIdWithClientInfo(1L))
                .thenReturn(Mono.just(clientInfo));
        when(notificationGateway.sendNotification(any(NotificationMessage.class)))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(notificationService.sendLoanDecisionNotification(approvedLoan, asesorEmail))
                .verifyComplete();

        // Verify all aspects of the notification
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationGateway).sendNotification(messageCaptor.capture());
        
        NotificationMessage sentMessage = messageCaptor.getValue();
        
        // Verify basic information
        assertThat(sentMessage.getEventType()).isEqualTo("LOAN_APPROVED");
        assertThat(sentMessage.getSolicitudId()).isEqualTo(1L);
        assertThat(sentMessage.getClientEmail()).isEqualTo("cliente.vip@empresa.com");
        assertThat(sentMessage.getClientName()).isEqualTo("Cliente VIP Premium");
        assertThat(sentMessage.getDecision()).isEqualTo("APPROVED");
        assertThat(sentMessage.getAsesorEmail()).isEqualTo(asesorEmail);
        
        // Verify approval details
        assertThat(sentMessage.getMontoAprobado()).contains("15,000,000");
        assertThat(sentMessage.getTasaInteres()).isEqualTo("16.2%");
        assertThat(sentMessage.getPlazoAprobado()).isEqualTo(48);
        assertThat(sentMessage.getPagoMensual()).contains("425,000");
        assertThat(sentMessage.getReason()).isEqualTo("Aprobación especial");
        
        // Verify metadata
        assertThat(sentMessage.getMessageId()).isNotNull();
        assertThat(sentMessage.getTimestamp()).isNotNull();
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private void testCurrencyFormatting(BigDecimal amount, String expectedFormat) {
        // Given
        LoanRequest approvedLoan = TestDataBuilder.approvedLoanRequest()
                .id(System.currentTimeMillis()) // Unique ID
                .approvedAmount(amount)
                .monthlyPayment(amount)
                .build();
        
        String asesorEmail = TestDataBuilder.SecurityContextBuilder.ADVISOR_EMAIL;
        
        LoanRequestReviewDTO clientInfo = TestDataBuilder.defaultReviewDTO()
                .email("cliente@test.com")
                .nombre("Test User")
                .build();

        when(loanRequestRepository.findByIdWithClientInfo(approvedLoan.getId()))
                .thenReturn(Mono.just(clientInfo));
        when(notificationGateway.sendNotification(any(NotificationMessage.class)))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(notificationService.sendLoanDecisionNotification(approvedLoan, asesorEmail))
                .verifyComplete();

        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationGateway).sendNotification(messageCaptor.capture());
        
        NotificationMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getMontoAprobado()).contains(expectedFormat);
        assertThat(sentMessage.getPagoMensual()).contains(expectedFormat);

        // Reset mocks for next test
        reset(loanRequestRepository, notificationGateway);
    }

    private void testPercentageFormatting(BigDecimal rate, String expectedFormat) {
        // Given
        LoanRequest approvedLoan = TestDataBuilder.approvedLoanRequest()
                .id(System.currentTimeMillis()) // Unique ID
                .interestRate(rate)
                .build();
        
        String asesorEmail = TestDataBuilder.SecurityContextBuilder.ADVISOR_EMAIL;
        
        LoanRequestReviewDTO clientInfo = TestDataBuilder.defaultReviewDTO()
                .email("cliente@test.com")
                .nombre("Test User")
                .build();

        when(loanRequestRepository.findByIdWithClientInfo(approvedLoan.getId()))
                .thenReturn(Mono.just(clientInfo));
        when(notificationGateway.sendNotification(any(NotificationMessage.class)))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(notificationService.sendLoanDecisionNotification(approvedLoan, asesorEmail))
                .verifyComplete();

        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationGateway).sendNotification(messageCaptor.capture());
        
        NotificationMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getTasaInteres()).isEqualTo(expectedFormat);

        // Reset mocks for next test
        reset(loanRequestRepository, notificationGateway);
    }
}
