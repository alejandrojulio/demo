package co.com.pragma.model.loan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para las decisiones de aprobación/rechazo de solicitudes de préstamo
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class LoanDecisionDTO {
    
    private Long solicitudId;
    private LoanRequest.LoanStatus decision; // APPROVED o REJECTED
    private String motivo; // Motivo de la decisión (obligatorio para rechazos)
    private BigDecimal montoAprobado; // Solo para aprobaciones
    private BigDecimal tasaInteres; // Solo para aprobaciones
    private Integer plazoAprobado; // Solo para aprobaciones (puede ser diferente al solicitado)
    private String asesorEmail; // Email del asesor que toma la decisión
}
