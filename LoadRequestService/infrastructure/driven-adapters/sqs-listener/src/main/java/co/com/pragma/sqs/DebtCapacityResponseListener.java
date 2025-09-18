package co.com.pragma.sqs;

import co.com.pragma.usecase.loan.UpdateLoanStatusUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Listener SQS SEPARADO - Recibe RESPONSES del DebtCapacityService
 * Cola separada para evitar competencia con el Lambda
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DebtCapacityResponseListener {

    private final UpdateLoanStatusUseCase updateLoanStatusUseCase;
    private final ObjectMapper objectMapper;

    /**
     * Procesa respuestas del DebtCapacityService en cola separada
     */
    @SqsListener(value = "${aws.sqs.debt-capacity-response-queue-url:https://sqs.us-east-2.amazonaws.com/882309621524/sqs_capacidad_response}")
    public void processResponseMessage(String message,
                                     @Header(value = "MessageType", required = false) String messageType) {
        
        log.info("🔔 LISTENER SQS ACTIVADO - Mensaje recibido: {}", message);
        log.info("🔔 Header MessageType: {}", messageType);
        
        try {
            // Parsear el mensaje JSON
            JsonNode messageNode = objectMapper.readTree(message);
            
            // Verificar tipo de mensaje (para compatibilidad con cola temporal)
            String eventType = messageNode.path("event_type").asText();
            log.info("🔔 Procesando mensaje SQS - EventType: {}", eventType);
            
            // Solo procesar respuestas, ignorar requests
            if (!"DEBT_CAPACITY_RESPONSE".equals(eventType)) {
                log.debug("Mensaje ignorado (no es respuesta): {}", eventType);
                return;
            }
            
            // Extraer datos de la respuesta - ESTRUCTURA SIMPLIFICADA
            Long solicitudId = messageNode.path("loan_request_id").asLong();
            String status = messageNode.path("status").asText();
            String reason = messageNode.path("reason").asText();
            String source = messageNode.path("source").asText("DebtCapacityService");
            
            log.info("📋 Procesando respuesta - Solicitud: {}, Estado: {}, Razón: {}", 
                    solicitudId, status, reason);
            
            // Actualizar el estado en la base de datos
            updateLoanStatusUseCase.updateLoanStatus(solicitudId, status, reason)
                .doOnSuccess(updatedLoan -> 
                    log.info("✅ Estado actualizado exitosamente por SQS - Solicitud: {}, Nuevo estado: {}", 
                            solicitudId, updatedLoan.getStatus()))
                .doOnError(error -> 
                    log.error("❌ Error actualizando estado por SQS - Solicitud: {}, Error: {}", 
                             solicitudId, error.getMessage(), error))
                .onErrorResume(error -> {
                    // Log error pero no fallar el procesamiento del mensaje SQS
                    log.error("Error procesando respuesta SQS para solicitud {}: {}", solicitudId, error.getMessage());
                    return Mono.empty();
                })
                .subscribe(); // Ejecutar de forma asíncrona
                
        } catch (Exception e) {
            log.error("Error parseando mensaje SQS de respuesta: {}", e.getMessage(), e);
            // No relanzar excepción para evitar que el mensaje vuelva a la cola
        }
    }
    
}
