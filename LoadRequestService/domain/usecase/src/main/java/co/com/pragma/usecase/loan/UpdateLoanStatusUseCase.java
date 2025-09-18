package co.com.pragma.usecase.loan;

import co.com.pragma.model.common.MessageFormatter;
import co.com.pragma.model.common.Messages;
import co.com.pragma.model.loan.LoanRequest;
import co.com.pragma.model.loan.gateways.LoanApplicationLogger;
import co.com.pragma.model.loan.gateways.LoanRequestRepository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Caso de uso para actualizar el estado de solicitudes desde la Lambda
 */
public class UpdateLoanStatusUseCase {

    private final LoanRequestRepository loanRequestRepository;
    private final LoanApplicationLogger logger;

    public UpdateLoanStatusUseCase(LoanRequestRepository loanRequestRepository, 
                                   LoanApplicationLogger logger) {
        this.loanRequestRepository = loanRequestRepository;
        this.logger = logger;
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
                                LoanRequest updatedLoan = existingLoan.toBuilder()
                                        .status(mappedStatus)
                                        .rejectionReason(mappedStatus == LoanRequest.LoanStatus.REJECTED ? reason : null)
                                        .updatedAt(LocalDateTime.now())
                                        .build();
                                
                                return loanRequestRepository.update(updatedLoan);
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
}
