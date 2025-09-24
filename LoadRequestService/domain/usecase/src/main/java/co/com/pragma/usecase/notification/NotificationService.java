package co.com.pragma.usecase.notification;

import co.com.pragma.model.common.MessageFormatter;
import co.com.pragma.model.common.Messages;
import co.com.pragma.model.loan.LoanRequest;
import co.com.pragma.model.loan.LoanRequestReviewDTO;
import co.com.pragma.model.loan.gateways.LoanApplicationLogger;
import co.com.pragma.model.loan.gateways.LoanRequestRepository;
import co.com.pragma.model.notification.NotificationMessage;
import co.com.pragma.model.notification.gateways.NotificationGateway;
import co.com.pragma.model.user.UserData;
import co.com.pragma.model.user.gateways.UserDataGateway;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

public class NotificationService {
    
    private final NotificationGateway notificationGateway;
    private final LoanApplicationLogger logger;
    private final LoanRequestRepository loanRequestRepository;
    private final UserDataGateway userDataGateway;
    private final NumberFormat currencyFormat;
    
    public NotificationService(NotificationGateway notificationGateway, 
                              LoanApplicationLogger logger,
                              LoanRequestRepository loanRequestRepository,
                              UserDataGateway userDataGateway) {
        this.notificationGateway = notificationGateway;
        this.logger = logger;
        this.loanRequestRepository = loanRequestRepository;
        this.userDataGateway = userDataGateway;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    }
    
    public Mono<Void> sendLoanDecisionNotification(LoanRequest loanRequest, String asesorEmail) {
        return loanRequestRepository.findByIdWithClientInfo(loanRequest.getId())
                .flatMap(loanWithClientInfo -> {
                    // Obtener los datos completos del cliente desde AuthService
                    return userDataGateway.getUserByDocument(loanWithClientInfo.getDocumentoCliente())
                            .map(userData -> {
                                // Actualizar el DTO con los datos del cliente
                                loanWithClientInfo.setEmail(userData.getEmail());
                                loanWithClientInfo.setNombre(userData.getFirstName() + " " + userData.getLastName());
                                return loanWithClientInfo;
                            })
                            .onErrorResume(error -> {
                                logger.error("Error obteniendo datos del cliente desde AuthService para documento " + 
                                           loanWithClientInfo.getDocumentoCliente() + ": " + error.getMessage(), error);
                                // Continuar con datos vacíos si no se puede obtener el cliente
                                return Mono.just(loanWithClientInfo);
                            });
                })
                .flatMap(loanWithClientInfo -> createNotificationMessage(loanRequest, loanWithClientInfo, asesorEmail))
                .flatMap(this::sendNotificationMessage)
                .doOnSuccess(result -> 
                    logger.info(MessageFormatter.format(Messages.NOTIFICATION_SENT_SUCCESS, 
                               loanRequest.getId().toString())))
                .doOnError(error -> 
                    logger.error(MessageFormatter.format(Messages.NOTIFICATION_SEND_FAILED, 
                               loanRequest.getId().toString(), error.getMessage())))
                .onErrorResume(error -> {
                    logger.error("Error al enviar notificación (continuando con el proceso): " + error.getMessage());
                    return Mono.empty();
                });
    }
    
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
                    .clientEmail(clientInfo.getEmail())
                    .clientName(clientInfo.getNombre())
                    .decision(loanRequest.getStatus().toString())
                    .asesorEmail(asesorEmail)
                    .timestamp(LocalDateTime.now());
            
            if (loanRequest.getStatus() == LoanRequest.LoanStatus.APPROVED) {
                builder.montoAprobado(formatCurrency(loanRequest.getApprovedAmount()))
                       .tasaInteres(formatPercentage(loanRequest.getInterestRate()))
                       .plazoAprobado(loanRequest.getTermInMonths())
                       .pagoMensual(formatCurrency(loanRequest.getMonthlyPayment()));
                       
                if (loanRequest.getNotes() != null) {
                    builder.reason(loanRequest.getNotes());
                }
            } else {
                builder.reason(loanRequest.getRejectionReason() != null ? 
                              loanRequest.getRejectionReason() : "No especificado");
            }
            
            NotificationMessage message = builder.build();
            
            logger.info(MessageFormatter.format(Messages.NOTIFICATION_MESSAGE_CREATED, 
                       loanRequest.getId().toString(), eventType));
            
            return message;
        });
    }
    
    private Mono<Void> sendNotificationMessage(NotificationMessage message) {
        return notificationGateway.sendNotification(message);
    }
    
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "N/A";
        return "COP $" + String.format("%,.2f", amount);
    }
    
    private String formatPercentage(BigDecimal percentage) {
        if (percentage == null) return "N/A";
        return percentage + "%";
    }
}
