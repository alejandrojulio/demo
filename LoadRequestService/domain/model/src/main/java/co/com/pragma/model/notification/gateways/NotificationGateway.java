package co.com.pragma.model.notification.gateways;

import co.com.pragma.model.notification.NotificationMessage;
import reactor.core.publisher.Mono;

/**
 * Gateway para envío de notificaciones
 */
public interface NotificationGateway {
    
    /**
     * Envía un mensaje de notificación a la cola SQS
     * @param message el mensaje de notificación a enviar
     * @return Mono<Void> que indica el resultado de la operación
     */
    Mono<Void> sendNotification(NotificationMessage message);
}
