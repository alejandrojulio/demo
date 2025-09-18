package co.com.pragma.model.debtcapacity.gateways;

import reactor.core.publisher.Mono;

/**
 * Gateway para comunicación con el servicio de capacidad de endeudamiento
 */
public interface DebtCapacityGateway {
    
    /**
     * Envía una solicitud de préstamo para validación automática de capacidad de endeudamiento
     * 
     * @param loanRequestId ID de la solicitud de préstamo
     * @return Mono que indica si el envío fue exitoso
     */
    Mono<Boolean> sendForAutomaticValidation(Long loanRequestId);
    
    /**
     * Envía una solicitud de validación directa (síncrona) de capacidad de endeudamiento
     * 
     * @param loanRequestId ID de la solicitud de préstamo
     * @return Mono con el resultado de la validación
     */
    Mono<DebtCapacityValidationResult> validateCapacityDirectly(Long loanRequestId);
}
