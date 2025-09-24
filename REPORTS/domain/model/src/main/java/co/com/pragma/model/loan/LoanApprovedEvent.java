package co.com.pragma.model.loan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)  // Ignorar campos desconocidos
public class LoanApprovedEvent {
    private String loanId;
    private String clientDocumentId;
    private BigDecimal approvedAmount;
    private BigDecimal interestRate;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private String loanType;
    private String eventType;
    private Integer termInMonths;        // Campo adicional que viene en el mensaje
    private BigDecimal monthlyPayment;   // Campo adicional que viene en el mensaje
    private String approvalType;         // MANUAL o AUTOMATIC
    
    public boolean isValidEvent() {
        return loanId != null && 
               !loanId.trim().isEmpty() && 
               "LOAN_APPROVED".equals(eventType);
    }
}
