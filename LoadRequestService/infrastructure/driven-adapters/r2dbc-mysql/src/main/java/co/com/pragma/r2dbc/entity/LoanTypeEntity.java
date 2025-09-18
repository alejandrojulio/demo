package co.com.pragma.r2dbc.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad de base de datos para tipos de préstamo
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("loan_types")
public class LoanTypeEntity {
    
    @Id
    @Column("id")
    private Integer id;
    
    @Column("type_code")
    private String typeCode;
    
    @Column("display_name")
    private String displayName;
    
    @Column("description")
    private String description;
    
    @Column("automatic_validation")
    private Boolean automaticValidation;
    
    @Column("default_interest_rate")
    private BigDecimal defaultInterestRate;
    
    @Column("min_amount")
    private BigDecimal minAmount;
    
    @Column("max_amount")
    private BigDecimal maxAmount;
    
    @Column("min_term_months")
    private Integer minTermMonths;
    
    @Column("max_term_months")
    private Integer maxTermMonths;
    
    @Column("is_active")
    private Boolean isActive;
    
    @Column("created_at")
    private LocalDateTime createdAt;
    
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
