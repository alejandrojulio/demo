package co.com.pragma.usecase.loan.gateways;

import co.com.pragma.model.loan.LoanRequest;
import reactor.core.publisher.Mono;

public interface LoanEventPublisher {
    Mono<Boolean> publishLoanApprovedEvent(LoanRequest approvedLoan);
    Mono<Boolean> publishLoanApprovedEvent(LoanRequest approvedLoan, String approvalType);
}
