package co.com.pragma.usecase.loan;

import co.com.pragma.model.common.MessageFormatter;
import co.com.pragma.model.common.Messages;
import co.com.pragma.model.loan.LoanRequest;
import co.com.pragma.model.loan.gateways.LoanApplicationLogger;
import co.com.pragma.model.loan.gateways.LoanRequestRepository;
import co.com.pragma.usecase.loan.gateways.LoanEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Caso de uso para actualizar el estado de solicitudes desde la Lambda
 */
public class UpdateLoanStatusUseCase {

    private final LoanRequestRepository loanRequestRepository;
    private final LoanApplicationLogger logger;
    private final LoanEventPublisher loanEventPublisher;

    public UpdateLoanStatusUseCase(LoanRequestRepository loanRequestRepository, 
                                   LoanApplicationLogger logger,
                                   LoanEventPublisher loanEventPublisher) {
        this.loanRequestRepository = loanRequestRepository;
        this.logger = logger;
        this.loanEventPublisher = loanEventPublisher;
    }

    @Transactional
    public Mono<LoanRequest> updateLoanStatus(Long loanRequestId, String newStatus, String reason) {
        logger.info("Actualizando estado de solicitud {} a {} por: {}", 
                    loanRequestId, newStatus, reason);

        return loanRequestRepository.findById(loanRequestId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                    MessageFormatter.format(Messages.VALIDATION_NOT_FOUND, "solicitud", "ID", loanRequestId))))
                .flatMap(existingLoan -> {
                    // Mapear el estado de la Lambda al enum del dominio
                    LoanRequest.LoanStatus mappedStatus = mapStringToLoanStatus(newStatus);
                    
                    // Validar que el cambio de estado sea válido
                    return validateStatusChange(existingLoan, mappedStatus)
                            .flatMap(valid -> {
                                if (!valid) {
                                    return Mono.error(new IllegalStateException(
                                        MessageFormatter.format("Cambio de estado inválido de {} a {}", 
                                                                existingLoan.getStatus(), mappedStatus)));
                                }
                                
                                // Crear la solicitud actualizada
                                LoanRequest.LoanRequestBuilder builder = existingLoan.toBuilder()
                                        .status(mappedStatus)
                                        .updatedAt(LocalDateTime.now());
                                
                                // Si es aprobación automática, completar campos de aprobación
                                if (mappedStatus == LoanRequest.LoanStatus.APPROVED) {
                                    builder.approvedAt(LocalDateTime.now())
                                           .approvedBy("Sistema Automático")
                                           .approvedAmount(existingLoan.getAmount()) // Usar el monto solicitado
                                           .interestRate(existingLoan.getInterestRate() != null ? 
                                                       existingLoan.getInterestRate() : 
                                                       java.math.BigDecimal.valueOf(15.0)); // Tasa por defecto
                                    
                                    // Calcular cuota mensual si no existe
                                    if (existingLoan.getMonthlyPayment() == null) {
                                        java.math.BigDecimal monthlyPayment = calculateMonthlyPayment(
                                            existingLoan.getAmount(),
                                            builder.build().getInterestRate(),
                                            existingLoan.getTermInMonths()
                                        );
                                        builder.monthlyPayment(monthlyPayment);
                                    }
                                    
                                    if (reason != null && !reason.trim().isEmpty()) {
                                        builder.notes(reason);
                                    }
                                } else if (mappedStatus == LoanRequest.LoanStatus.REJECTED) {
                                    builder.rejectionReason(reason);
                                }
                                
                                LoanRequest updatedLoan = builder.build();
                                
                                return loanRequestRepository.update(updatedLoan)
                                        .flatMap(savedLoan -> {
                                            // Si fue aprobado automáticamente, enviar evento
                                            if (mappedStatus == LoanRequest.LoanStatus.APPROVED) {
                                                return loanEventPublisher.publishLoanApprovedEvent(savedLoan, "AUTOMATIC")
                                                        .doOnNext(success -> {
                                                            if (success) {
                                                                logger.info("✅ Evento de préstamo aprobado AUTOMÁTICAMENTE enviado para solicitud: {}", savedLoan.getId());
                                                            } else {
                                                                logger.warn("⚠️ No se pudo enviar evento de préstamo aprobado automático para solicitud: {}", savedLoan.getId());
                                                            }
                                                        })
                                                        .thenReturn(savedLoan);
                                            }
                                            return Mono.just(savedLoan);
                                        });
                            });
                })
                .doOnSuccess(updatedLoan -> 
                    logger.info("Estado actualizado exitosamente para solicitud {}: {}", 
                                loanRequestId, updatedLoan.getStatus()))
                .doOnError(error -> 
                    logger.error("Error actualizando estado para solicitud " + loanRequestId + ": " + error.getMessage(), error));
    }

    /**
     * Mapea string de estado de la Lambda al enum del dominio
     */
    private LoanRequest.LoanStatus mapStringToLoanStatus(String status) {
        switch (status.toUpperCase()) {
            case "APPROVED": return LoanRequest.LoanStatus.APPROVED;
            case "REJECTED": return LoanRequest.LoanStatus.REJECTED;
            case "MANUAL_REVIEW": return LoanRequest.LoanStatus.MANUAL_REVIEW;
            case "PENDING_REVIEW": return LoanRequest.LoanStatus.PENDING_REVIEW;
            case "CANCELLED": return LoanRequest.LoanStatus.CANCELLED;
            default:
                logger.warn("Estado desconocido recibido: {}. Usando PENDING_REVIEW", status);
                return LoanRequest.LoanStatus.PENDING_REVIEW;
        }
    }

    /**
     * Valida que el cambio de estado sea permitido
     */
    private Mono<Boolean> validateStatusChange(LoanRequest currentLoan, LoanRequest.LoanStatus newStatus) {
        LoanRequest.LoanStatus currentStatus = currentLoan.getStatus();
        
        // Reglas de negocio para cambios de estado válidos
        switch (currentStatus) {
            case PENDING_REVIEW:
                // Desde PENDING_REVIEW se puede ir a cualquier estado
                return Mono.just(true);
                
            case MANUAL_REVIEW:
                // Desde MANUAL_REVIEW se puede aprobar o rechazar
                return Mono.just(newStatus == LoanRequest.LoanStatus.APPROVED || 
                                newStatus == LoanRequest.LoanStatus.REJECTED ||
                                newStatus == LoanRequest.LoanStatus.CANCELLED);
                
            case APPROVED:
                // Una vez aprobada, solo se puede cancelar
                return Mono.just(newStatus == LoanRequest.LoanStatus.CANCELLED);
                
            case REJECTED:
                // Una vez rechazada, no se puede cambiar
                logger.warn("Intento de cambiar estado de solicitud ya rechazada {}", currentLoan.getId());
                return Mono.just(false);
                
            case CANCELLED:
                // Una vez cancelada, no se puede cambiar
                logger.warn("Intento de cambiar estado de solicitud cancelada {}", currentLoan.getId());
                return Mono.just(false);
                
            default:
                logger.warn("Estado actual desconocido: {}", currentStatus);
                return Mono.just(false);
        }
    }
    
    /**
     * Calcula la cuota mensual usando la fórmula de amortización francesa
     */
    private java.math.BigDecimal calculateMonthlyPayment(java.math.BigDecimal principal, 
                                                        java.math.BigDecimal annualRate, 
                                                        Integer termMonths) {
        if (principal == null || annualRate == null || termMonths == null || termMonths <= 0) {
            return java.math.BigDecimal.ZERO;
        }
        
        // Tasa mensual = tasa anual / 12 / 100
        java.math.BigDecimal monthlyRate = annualRate.divide(java.math.BigDecimal.valueOf(12), 6, java.math.RoundingMode.HALF_UP)
                                                     .divide(java.math.BigDecimal.valueOf(100), 6, java.math.RoundingMode.HALF_UP);
        
        if (monthlyRate.compareTo(java.math.BigDecimal.ZERO) == 0) {
            // Si no hay interés, solo dividir principal entre meses
            return principal.divide(java.math.BigDecimal.valueOf(termMonths), 2, java.math.RoundingMode.HALF_UP);
        }
        
        // Fórmula: M = P * (r * (1 + r)^n) / ((1 + r)^n - 1)
        java.math.BigDecimal onePlusRate = java.math.BigDecimal.ONE.add(monthlyRate);
        java.math.BigDecimal poweredRate = onePlusRate.pow(termMonths);
        
        java.math.BigDecimal numerator = principal.multiply(monthlyRate).multiply(poweredRate);
        java.math.BigDecimal denominator = poweredRate.subtract(java.math.BigDecimal.ONE);
        
        return numerator.divide(denominator, 2, java.math.RoundingMode.HALF_UP);
    }
}
