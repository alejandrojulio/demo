package co.com.pragma.model.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Mensaje de notificación para enviar a la cola SQS
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    
    private String messageId;
    private String eventType; // LOAN_APPROVED, LOAN_REJECTED
    private Long solicitudId;
    private String clientEmail;
    private String clientName;
    private String decision; // APPROVED, REJECTED
    private String reason; // Para rechazos
    private String asesorEmail;
    private LocalDateTime timestamp;
    
    // Campos específicos para aprobaciones
    private String montoAprobado;
    private String tasaInteres;
    private Integer plazoAprobado;
    private String pagoMensual;
}
