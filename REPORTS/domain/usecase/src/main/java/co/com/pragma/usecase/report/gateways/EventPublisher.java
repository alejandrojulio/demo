package co.com.pragma.usecase.report.gateways;

import reactor.core.publisher.Mono;

public interface EventPublisher {
    Mono<Void> publishReportUpdated(Long totalLoans, String lastLoanId);
}
