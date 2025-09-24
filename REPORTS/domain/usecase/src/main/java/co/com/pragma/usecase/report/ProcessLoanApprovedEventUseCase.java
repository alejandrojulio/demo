package co.com.pragma.usecase.report;

import co.com.pragma.model.common.MessageFormatter;
import co.com.pragma.model.common.Messages;
import co.com.pragma.model.loan.LoanApprovedEvent;
import co.com.pragma.model.report.LoanReport;
import co.com.pragma.usecase.report.gateways.LoanReportRepository;
import co.com.pragma.usecase.report.gateways.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Slf4j
public class ProcessLoanApprovedEventUseCase {
    
    private final LoanReportRepository loanReportRepository;
    private final EventPublisher eventPublisher;
    
    public Mono<LoanReport> processLoanApprovedEvent(LoanApprovedEvent event) {
        log.info(MessageFormatter.format(Messages.LOG_EVENT_RECEIVED, 
                event.getEventType(), event.getLoanId()));
        
        if (!event.isValidEvent()) {
            log.warn("Evento inválido recibido: {}", event);
            return Mono.error(new IllegalArgumentException(Messages.ERROR_INVALID_EVENT));
        }
        
        log.info(MessageFormatter.format(Messages.LOG_OPERATION_STARTED, 
                "procesamiento de evento", "préstamo aprobado"));
        
        String approvalType = event.getApprovalType() != null ? event.getApprovalType() : "MANUAL";
        java.math.BigDecimal approvedAmount = event.getApprovedAmount() != null ? event.getApprovedAmount() : java.math.BigDecimal.ZERO;
        
        return loanReportRepository.updateCounter(event.getLoanId(), approvalType, approvedAmount)
                .doOnNext(updatedReport -> {
                    log.info("🔄 Contador actualizado - Total préstamos: {}, Monto total: ${}, Último ID: {} (Tipo: {})", 
                            updatedReport.getTotalApprovedLoans(), 
                            updatedReport.getTotalApprovedAmount(),
                            updatedReport.getLastLoanId(),
                            approvalType);
                })
                .flatMap(updatedReport -> {
                    // Opcional: publicar evento de que el reporte fue actualizado
                    return eventPublisher.publishReportUpdated(
                            updatedReport.getTotalApprovedLoans(), 
                            updatedReport.getLastLoanId())
                            .thenReturn(updatedReport);
                })
                .doOnNext(report -> {
                    log.info(MessageFormatter.format(Messages.LOG_OPERATION_COMPLETED, 
                            "procesamiento de evento", "préstamo aprobado"));
                })
                .onErrorResume(Exception.class, error -> {
                    log.error("Error al procesar evento de préstamo aprobado", error);
                    return Mono.error(new RuntimeException(Messages.ERROR_SQS_PROCESSING, error));
                });
    }
}
