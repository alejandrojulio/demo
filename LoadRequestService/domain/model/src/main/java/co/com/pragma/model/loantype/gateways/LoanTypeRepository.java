package co.com.pragma.model.loantype.gateways;

import co.com.pragma.model.loantype.LoanType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository para gestión de tipos de préstamo
 */
public interface LoanTypeRepository {
    
    /**
     * Busca un tipo de préstamo por su código
     */
    Mono<LoanType> findByTypeCode(LoanType.LoanTypeCode typeCode);
    
    /**
     * Obtiene todos los tipos de préstamo activos
     */
    Flux<LoanType> findAllActive();
    
    /**
     * Obtiene todos los tipos de préstamo con validación automática habilitada
     */
    Flux<LoanType> findByAutomaticValidationEnabled();
    
    /**
     * Verifica si un tipo de préstamo requiere validación automática
     */
    Mono<Boolean> requiresAutomaticValidation(LoanType.LoanTypeCode typeCode);
}
