package co.com.pragma.model.loan;

import lombok.*;

import java.math.BigDecimal;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRequestDTO {
    
    private String clientDocumentId;
    private BigDecimal amount;
    private Integer termInMonths;
    private LoanRequest.LoanType loanType;
    private String notes;
}
