package co.com.pragma.events;

import co.com.pragma.usecase.report.gateways.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class LogEventPublisher implements EventPublisher {
    
    @Override
    public Mono<Void> publishReportUpdated(Long totalLoans, String lastLoanId) {
        log.info("📊 EVENTO: Reporte actualizado - Total préstamos: {}, Último préstamo: {}", 
                totalLoans, lastLoanId);
        
        // En una implementación real, aquí se podría publicar a un topic SNS o SQS
        // Por ahora solo logueamos el evento
        
        return Mono.empty();
    }
}
