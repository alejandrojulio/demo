package co.com.pragma.r2dbc.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Projection funcional para consultas de solicitudes en revisión
 * Mapeo automático de Spring Data R2DBC
 */
public interface LoanRequestReviewProjection {
    
    // Datos de la solicitud
    Long getId();
    BigDecimal getAmount();
    Integer getTermInMonths();
    String getLoanType();
    String getStatus();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
    String getClientDocument();
    String getNotes();
    
    // Datos del usuario (JOIN)
    String getEmail();
    String getFirstName();
    String getLastName();
    BigDecimal getBaseSalary();
    
    // Campos calculados
    BigDecimal getInterestRate();
    BigDecimal getMonthlyDebt();
}
