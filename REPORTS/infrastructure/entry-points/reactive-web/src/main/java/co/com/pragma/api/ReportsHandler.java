package co.com.pragma.api;

import co.com.pragma.api.dto.ApiResponse;
import co.com.pragma.api.dto.LoanReportDTO;
import co.com.pragma.model.common.MessageFormatter;
import co.com.pragma.model.common.Messages;
import co.com.pragma.usecase.report.GetLoanReportUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportsHandler {
    
    private final GetLoanReportUseCase getLoanReportUseCase;
    
    public Mono<ServerResponse> getApprovedLoansReport(ServerRequest serverRequest) {
        log.info(MessageFormatter.format(Messages.LOG_OPERATION_STARTED, 
                "consulta de reporte", "préstamos aprobados"));
        
        return getLoanReportUseCase.getLoanReport()
                .map(LoanReportDTO::fromLoanReport)
                .flatMap(reportDTO -> {
                    log.info(MessageFormatter.format(Messages.LOG_REPORT_RETRIEVED, 
                            reportDTO.getTotalApprovedLoans()));
                    
                    ApiResponse<LoanReportDTO> response = ApiResponse.success(
                            reportDTO,
                            Messages.REPORT_RETRIEVED
                    );
                    
                    return ServerResponse.ok().bodyValue(response);
                })
                .onErrorResume(Exception.class, this::handleError)
                .doFinally(signalType -> {
                    log.info(MessageFormatter.format(Messages.LOG_OPERATION_COMPLETED, 
                            "consulta de reporte", "préstamos aprobados"));
                });
    }
    
    public Mono<ServerResponse> getHealthCheck(ServerRequest serverRequest) {
        log.debug("Health check solicitado");
        
        return ServerResponse.ok()
                .bodyValue(ApiResponse.success("OK", "Servicio de reportes funcionando correctamente"));
    }
    
    private Mono<ServerResponse> handleError(Exception error) {
        log.error("Error en ReportsHandler", error);
        
        String errorMessage;
        HttpStatus status;
        
        if (error instanceof IllegalArgumentException) {
            errorMessage = error.getMessage();
            status = HttpStatus.BAD_REQUEST;
        } else if (error.getMessage() != null && error.getMessage().contains("DynamoDB")) {
            errorMessage = Messages.ERROR_DYNAMODB_CONNECTION;
            status = HttpStatus.SERVICE_UNAVAILABLE;
        } else {
            errorMessage = Messages.ERROR_UNEXPECTED;
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        
        ApiResponse<String> response = ApiResponse.error(errorMessage);
        
        return ServerResponse.status(status).bodyValue(response);
    }
}
