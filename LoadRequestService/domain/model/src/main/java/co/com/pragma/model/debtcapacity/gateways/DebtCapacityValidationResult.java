package co.com.pragma.model.debtcapacity.gateways;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Resultado de la validación de capacidad de endeudamiento
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebtCapacityValidationResult {
    
    private Long loanRequestId;
    private String decision; // APROBADO, RECHAZADO, REVISION_MANUAL
    private String reason;
    private BigDecimal maxDebtCapacity;
    private BigDecimal currentMonthlyDebt;
    private BigDecimal availableCapacity;
    private BigDecimal newLoanPayment;
    private boolean success;
    private String errorMessage;
    
    public static DebtCapacityValidationResult success(Long loanRequestId, String decision, String reason,
                                                      BigDecimal maxCapacity, BigDecimal currentDebt,
                                                      BigDecimal availableCapacity, BigDecimal newPayment) {
        return DebtCapacityValidationResult.builder()
                .loanRequestId(loanRequestId)
                .decision(decision)
                .reason(reason)
                .maxDebtCapacity(maxCapacity)
                .currentMonthlyDebt(currentDebt)
                .availableCapacity(availableCapacity)
                .newLoanPayment(newPayment)
                .success(true)
                .build();
    }
    
    public static DebtCapacityValidationResult error(Long loanRequestId, String errorMessage) {
        return DebtCapacityValidationResult.builder()
                .loanRequestId(loanRequestId)
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
