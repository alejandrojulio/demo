package co.com.pragma.usecase.loan;

import co.com.pragma.model.common.MessageFormatter;
import co.com.pragma.model.common.Messages;
import co.com.pragma.model.loan.LoanDecisionDTO;
import co.com.pragma.model.loan.LoanRequest;
import co.com.pragma.model.loan.gateways.LoanApplicationLogger;
import co.com.pragma.model.loan.gateways.LoanRequestRepository;
import co.com.pragma.usecase.notification.NotificationService;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public class ProcessLoanDecisionUseCase {

    private final LoanRequestRepository loanRequestRepository;
    private final LoanApplicationLogger logger;
    private final NotificationService notificationService;

    public ProcessLoanDecisionUseCase(LoanRequestRepository loanRequestRepository, 
                                     LoanApplicationLogger logger,
                                     NotificationService notificationService) {
        this.loanRequestRepository = loanRequestRepository;
        this.logger = logger;
        this.notificationService = notificationService;
    }

    public Mono<LoanRequest> processDecision(LoanDecisionDTO decision) {
        return processDecision(decision, null);
    }
    
    @Transactional
    public Mono<LoanRequest> processDecision(LoanDecisionDTO decision, String asesorId) {
        logger.info(MessageFormatter.format(Messages.LOG_OPERATION_STARTED, 
                   "procesamiento de decisión", 
                   "solicitud: " + decision.getSolicitudId() + " por asesor: " + decision.getAsesorEmail()));

        return validateDecision(decision)
                .flatMap(this::findExistingRequest)
                .flatMap(request -> this.updateRequestWithDecision(request, decision, asesorId))
                .flatMap(loanRequestRepository::save)
                .flatMap(savedRequest -> 
                    notificationService.sendLoanDecisionNotification(savedRequest, decision.getAsesorEmail())
                            .thenReturn(savedRequest)
                )
                .doOnSuccess(result -> 
                    logger.info(MessageFormatter.format(Messages.LOG_OPERATION_COMPLETED, 
                               "procesamiento de decisión", 
                               "solicitud: " + result.getId() + ", estado: " + result.getStatus() + ", asesor: " + decision.getAsesorEmail())))
                .doOnError(error -> 
                    logger.error(MessageFormatter.format(Messages.LOG_OPERATION_FAILED, 
                               "procesamiento de decisión", 
                               "solicitud: " + decision.getSolicitudId() + ", asesor: " + decision.getAsesorEmail(),
                               error.getMessage())));
    }

    private Mono<LoanDecisionDTO> validateDecision(LoanDecisionDTO decision) {
        if (decision == null) {
            return Mono.error(new IllegalArgumentException(MessageFormatter.format(Messages.VALIDATION_REQUIRED_FIELD, "decisión")));
        }

        if (decision.getSolicitudId() == null) {
            return Mono.error(new IllegalArgumentException(MessageFormatter.format(Messages.VALIDATION_REQUIRED_FIELD, "ID de la solicitud")));
        }

        if (decision.getDecision() == null) {
            return Mono.error(new IllegalArgumentException(Messages.LOAN_DECISION_VALIDATION_REQUIRED));
        }

        if (decision.getDecision() != LoanRequest.LoanStatus.APPROVED && decision.getDecision() != LoanRequest.LoanStatus.REJECTED) {
            return Mono.error(new IllegalArgumentException(Messages.LOAN_DECISION_VALIDATION_INVALID));
        }

        if (decision.getAsesorEmail() == null || decision.getAsesorEmail().trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException(MessageFormatter.format(Messages.VALIDATION_REQUIRED_FIELD, "email del asesor")));
        }

        if (decision.getDecision() == LoanRequest.LoanStatus.APPROVED) {
            if (decision.getMontoAprobado() == null || decision.getMontoAprobado().compareTo(BigDecimal.ZERO) <= 0) {
                return Mono.error(new IllegalArgumentException(Messages.LOAN_APPROVAL_AMOUNT_REQUIRED));
            }

            if (decision.getTasaInteres() == null || decision.getTasaInteres().compareTo(BigDecimal.ZERO) <= 0) {
                return Mono.error(new IllegalArgumentException(Messages.LOAN_APPROVAL_RATE_REQUIRED));
            }

            if (decision.getPlazoAprobado() == null || decision.getPlazoAprobado() <= 0) {
                return Mono.error(new IllegalArgumentException(Messages.LOAN_APPROVAL_TERM_REQUIRED));
            }
        }

        if (decision.getDecision() == LoanRequest.LoanStatus.REJECTED) {
            if (decision.getMotivo() == null || decision.getMotivo().trim().isEmpty()) {
                return Mono.error(new IllegalArgumentException(Messages.LOAN_REJECTION_REASON_REQUIRED));
            }
        }

        return Mono.just(decision);
    }

    private Mono<LoanRequest> findExistingRequest(LoanDecisionDTO decision) {
        return loanRequestRepository.findById(decision.getSolicitudId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                    MessageFormatter.format(Messages.VALIDATION_NOT_FOUND, "solicitud", "ID", decision.getSolicitudId()))))
                .flatMap(request -> {
                    if (request.getStatus() == LoanRequest.LoanStatus.APPROVED) {
                        return Mono.error(new IllegalStateException(Messages.LOAN_VALIDATION_STATE_APPROVED));
                    }
                    if (request.getStatus() == LoanRequest.LoanStatus.REJECTED) {
                        return Mono.error(new IllegalStateException(Messages.LOAN_VALIDATION_STATE_REJECTED));
                    }
                    if (request.getStatus() == LoanRequest.LoanStatus.CANCELLED) {
                        return Mono.error(new IllegalStateException(Messages.LOAN_VALIDATION_STATE_CANCELLED));
                    }

                    return Mono.just(request);
                });
    }

    private Mono<LoanRequest> updateRequestWithDecision(LoanRequest request, LoanDecisionDTO decision) {
        return updateRequestWithDecision(request, decision, null);
    }
    
    private Mono<LoanRequest> updateRequestWithDecision(LoanRequest request, LoanDecisionDTO decision, String asesorId) {
        return Mono.fromCallable(() -> {
            LoanRequest.LoanRequestBuilder builder = request.toBuilder()
                    .status(decision.getDecision())
                    .updatedAt(LocalDateTime.now());

            if (decision.getDecision() == LoanRequest.LoanStatus.APPROVED) {
                builder.approvedAmount(decision.getMontoAprobado())
                       .interestRate(decision.getTasaInteres())
                       .termInMonths(decision.getPlazoAprobado())
                       .approvedAt(LocalDateTime.now())
                       .approvedBy(asesorId != null ? asesorId : decision.getAsesorEmail());

                BigDecimal monthlyPayment = calculateMonthlyPayment(
                    decision.getMontoAprobado(), 
                    decision.getTasaInteres(), 
                    decision.getPlazoAprobado()
                );
                builder.monthlyPayment(monthlyPayment);

                if (decision.getMotivo() != null && !decision.getMotivo().trim().isEmpty()) {
                    builder.notes(decision.getMotivo());
                }

            } else if (decision.getDecision() == LoanRequest.LoanStatus.REJECTED) {
                builder.rejectionReason(decision.getMotivo())
                       .notes(decision.getMotivo());
            }

            return builder.build();
        });
    }

    // Fórmula de amortización francesa
    private BigDecimal calculateMonthlyPayment(BigDecimal amount, BigDecimal annualRate, Integer months) {
        if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
            return amount.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        }

        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(100 * 12), 10, RoundingMode.HALF_UP);
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal power = onePlusRate.pow(months);
        
        BigDecimal numerator = amount.multiply(monthlyRate).multiply(power);
        BigDecimal denominator = power.subtract(BigDecimal.ONE);
        
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }
}
