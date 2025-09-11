package co.com.pragma.model.loan;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para representar las solicitudes que necesitan revisión manual
 * Incluye información del préstamo y datos del cliente
 */
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRequestReviewDTO {
    
    // Información de la solicitud
    private Long id;
    private BigDecimal monto;
    private Integer plazo;
    private String email;
    private String nombre;
    private String tipoPrestamo;
    private BigDecimal tasaInteres;
    private String estadoSolicitud;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    
    // Información financiera del cliente
    private BigDecimal salarioBase;
    private BigDecimal deudaTotalMensualSolicitudesAprobadas;
    
    // Información adicional para el asesor
    private String documentoCliente;
    private String notas;
}
