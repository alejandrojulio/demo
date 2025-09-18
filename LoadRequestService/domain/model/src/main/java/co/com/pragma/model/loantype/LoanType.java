package co.com.pragma.model.loantype;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad que representa un tipo de préstamo con su configuración
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanType {
    
    private Integer id;
    private LoanTypeCode typeCode;
    private String displayName;
    private String description;
    private Boolean automaticValidation;
    private BigDecimal defaultInterestRate;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private Integer minTermMonths;
    private Integer maxTermMonths;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public enum LoanTypeCode {
        PERSONAL("PERSONAL", "Préstamo Personal"),
        VEHICLE("VEHICLE", "Préstamo Vehicular"),
        HOME("HOME", "Préstamo Hipotecario"),
        BUSINESS("BUSINESS", "Préstamo Empresarial");
        
        private final String code;
        private final String displayName;
        
        LoanTypeCode(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public static LoanTypeCode fromCode(String code) {
            for (LoanTypeCode loanType : values()) {
                if (loanType.code.equals(code)) {
                    return loanType;
                }
            }
            throw new IllegalArgumentException("Código de tipo de préstamo no válido: " + code);
        }
    }
    
    /**
     * Verifica si este tipo de préstamo requiere validación automática
     */
    public boolean requiresAutomaticValidation() {
        return Boolean.TRUE.equals(automaticValidation);
    }
    
    /**
     * Verifica si el monto está dentro del rango permitido para este tipo
     */
    public boolean isValidAmount(BigDecimal amount) {
        return amount != null && 
               amount.compareTo(minAmount) >= 0 && 
               amount.compareTo(maxAmount) <= 0;
    }
    
    /**
     * Verifica si el plazo está dentro del rango permitido para este tipo
     */
    public boolean isValidTerm(Integer termMonths) {
        return termMonths != null && 
               termMonths >= minTermMonths && 
               termMonths <= maxTermMonths;
    }
}
