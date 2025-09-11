package co.com.pragma.model.loan;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class LoanRequest {
    private Long id;
    private String clientDocumentId;
    private BigDecimal amount;
    private Integer termInMonths;
    private LoanType loanType;
    private LoanStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String notes;
    
    private BigDecimal approvedAmount;
    private BigDecimal interestRate;
    private BigDecimal monthlyPayment;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private String rejectionReason;
    
    public enum LoanType {
        PERSONAL("Personal"),
        VEHICLE("Vehículo"),
        HOME("Vivienda"),
        BUSINESS("Negocio");
        
        private final String displayName;
        
        LoanType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum LoanStatus {
        PENDING_REVIEW("Pendiente de revisión"),
        APPROVED("Aprobada"),
        REJECTED("Rechazada"),
        CANCELLED("Cancelada"),
        MANUAL_REVIEW("Revisión manual");
        
        private final String displayName;
        
        LoanStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public boolean isValidAmount() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public boolean isValidTerm() {
        return termInMonths != null && termInMonths > 0 && termInMonths <= 120;
    }
    
    public boolean isPendingReview() {
        return LoanStatus.PENDING_REVIEW.equals(this.status);
    }
}
