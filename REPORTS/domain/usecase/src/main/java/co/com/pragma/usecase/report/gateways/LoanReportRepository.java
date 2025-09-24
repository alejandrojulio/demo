package co.com.pragma.usecase.report.gateways;

import co.com.pragma.model.report.LoanReport;
import reactor.core.publisher.Mono;

public interface LoanReportRepository {
    Mono<LoanReport> findReport();
    Mono<LoanReport> saveReport(LoanReport report);
    Mono<LoanReport> updateCounter(String loanId);
    Mono<LoanReport> updateCounter(String loanId, String approvalType);
    Mono<LoanReport> updateCounter(String loanId, String approvalType, java.math.BigDecimal approvedAmount);
}
