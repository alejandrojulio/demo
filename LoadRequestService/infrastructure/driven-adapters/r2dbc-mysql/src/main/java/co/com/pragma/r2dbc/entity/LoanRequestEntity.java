package co.com.pragma.r2dbc.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("loan_requests")
public class LoanRequestEntity {
    
    @Id
    @Column("id")
    private Long id;
    
    @Column("client_document")
    private String clientDocumentId;
    
    @Column("amount")
    private BigDecimal amount;
    
    @Column("term_in_months")
    private Integer termInMonths;
    
    @Column("loan_type")
    private String loanType;
    
    @Column("status")
    private String status;
    
    @Column("created_at")
    private LocalDateTime createdAt;
    
    @Column("updated_at")
    private LocalDateTime updatedAt;
    
    @Column("notes")
    private String notes;
    
    // Campos adicionales para evaluación/aprobación
    @Column("interest_rate")
    private BigDecimal interestRate;
    
    @Column("monthly_payment")
    private BigDecimal monthlyPayment;
    
    @Column("approved_amount")
    private BigDecimal approvedAmount;
    
    @Column("approved_at")
    private LocalDateTime approvedAt;
    
    @Column("approved_by")
    private String approvedBy;
    
    @Column("rejection_reason")
    private String rejectionReason;
}
