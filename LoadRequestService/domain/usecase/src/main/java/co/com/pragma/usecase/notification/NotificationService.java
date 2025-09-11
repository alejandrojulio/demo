package co.com.pragma.usecase.notification;

import co.com.pragma.model.common.MessageFormatter;
import co.com.pragma.model.common.Messages;
import co.com.pragma.model.loan.LoanRequest;
import co.com.pragma.model.loan.LoanRequestReviewDTO;
import co.com.pragma.model.loan.gateways.LoanApplicationLogger;
import co.com.pragma.model.loan.gateways.LoanRequestRepository;
import co.com.pragma.model.notification.NotificationMessage;
import co.com.pragma.model.notification.gateways.NotificationGateway;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * Servicio para crear y enviar notificaciones de decisiones de préstamo
 */
public class NotificationService {
    
    private final NotificationGateway notificationGateway;
    private final LoanApplicationLogger logger;
    private final LoanRequestRepository loanRequestRepository;
    private final NumberFormat currencyFormat;
    
    public NotificationService(NotificationGateway notificationGateway, 
                              LoanApplicationLogger logger,
                              LoanRequestRepository loanRequestRepository) {
        this.notificationGateway = notificationGateway;
        this.logger = logger;
        this.loanRequestRepository = loanRequestRepository;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    }
    
    /**
     * Envía una notificación cuando se toma una decisión sobre una solicitud de préstamo
     */
    public Mono<Void> sendLoanDecisionNotification(LoanRequest loanRequest, String asesorEmail) {
        // Obtener información completa del cliente desde la base de datos
        return loanRequestRepository.findByIdWithClientInfo(loanRequest.getId())
                .flatMap(loanWithClientInfo -> createNotificationMessage(loanRequest, loanWithClientInfo, asesorEmail))
                .flatMap(this::sendNotificationMessage)
                .doOnSuccess(result -> 
                    logger.info(MessageFormatter.format(Messages.NOTIFICATION_SENT_SUCCESS, 
                               loanRequest.getId().toString())))
                .doOnError(error -> 
                    logger.error(MessageFormatter.format(Messages.NOTIFICATION_SEND_FAILED, 
                               loanRequest.getId().toString(), error.getMessage())))
                .onErrorResume(error -> {
                    // Log el error pero no fallar el proceso principal
                    logger.error("Error al enviar notificación (continuando con el proceso): " + error.getMessage());
                    return Mono.empty();
                });
    }
    
    /**
     * Crea el mensaje de notificación basado en la decisión tomada
     */
    private Mono<NotificationMessage> createNotificationMessage(LoanRequest loanRequest, 
                                                               LoanRequestReviewDTO clientInfo, 
                                                               String asesorEmail) {
        return Mono.fromCallable(() -> {
            String eventType = loanRequest.getStatus() == LoanRequest.LoanStatus.APPROVED ? 
                               "LOAN_APPROVED" : "LOAN_REJECTED";
            
            NotificationMessage.NotificationMessageBuilder builder = NotificationMessage.builder()
                    .messageId(UUID.randomUUID().toString())
                    .eventType(eventType)
                    .solicitudId(loanRequest.getId())
                    .clientEmail(clientInfo.getEmail()) // Email real de la base de datos
                    .clientName(clientInfo.getNombre()) // Nombre real de la base de datos
                    .decision(loanRequest.getStatus().toString())
                    .asesorEmail(asesorEmail)
                    .timestamp(LocalDateTime.now());
            
            if (loanRequest.getStatus() == LoanRequest.LoanStatus.APPROVED) {
                // Para aprobaciones, incluir detalles del préstamo
                builder.montoAprobado(formatCurrency(loanRequest.getApprovedAmount()))
                       .tasaInteres(formatPercentage(loanRequest.getInterestRate()))
                       .plazoAprobado(loanRequest.getTermInMonths())
                       .pagoMensual(formatCurrency(loanRequest.getMonthlyPayment()));
                       
                if (loanRequest.getNotes() != null) {
                    builder.reason(loanRequest.getNotes());
                }
            } else {
                // Para rechazos, incluir motivo
                builder.reason(loanRequest.getRejectionReason() != null ? 
                              loanRequest.getRejectionReason() : "No especificado");
            }
            
            NotificationMessage message = builder.build();
            
            logger.info(MessageFormatter.format(Messages.NOTIFICATION_MESSAGE_CREATED, 
                       loanRequest.getId().toString(), eventType));
            
            return message;
        });
    }
    
    /**
     * Envía el mensaje de notificación a través del gateway
     */
    private Mono<Void> sendNotificationMessage(NotificationMessage message) {
        return notificationGateway.sendNotification(message);
    }
    
    /**
     * Formatea un valor monetario
     */
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "N/A";
        return currencyFormat.format(amount);
    }
    
    /**
     * Formatea un porcentaje
     */
    private String formatPercentage(BigDecimal percentage) {
        if (percentage == null) return "N/A";
        return percentage + "%";
    }
}
