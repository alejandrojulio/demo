package co.com.pragma.usecase.report;

import co.com.pragma.model.common.MessageFormatter;
import co.com.pragma.model.common.Messages;
import co.com.pragma.model.report.LoanReport;
import co.com.pragma.usecase.report.gateways.LoanReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Slf4j
public class GetLoanReportUseCase {
    
    private final LoanReportRepository loanReportRepository;
    
    public Mono<LoanReport> getLoanReport() {
        log.info(MessageFormatter.format(Messages.LOG_OPERATION_STARTED, 
                "consulta de reporte", "préstamos aprobados"));
        
        return loanReportRepository.findReport()
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Reporte no encontrado, creando uno nuevo");
                    LoanReport newReport = LoanReport.createNew();
                    return loanReportRepository.saveReport(newReport);
                }))
                .doOnNext(report -> {
                    log.info(MessageFormatter.format(Messages.LOG_REPORT_RETRIEVED, 
                            report.getTotalApprovedLoans()));
                })
                .doOnNext(report -> {
                    log.info(MessageFormatter.format(Messages.LOG_OPERATION_COMPLETED, 
                            "consulta de reporte", "préstamos aprobados"));
                })
                .onErrorResume(Exception.class, error -> {
                    log.error("Error al obtener el reporte de préstamos", error);
                    return Mono.error(new RuntimeException(Messages.ERROR_DYNAMODB_CONNECTION, error));
                });
    }
}
