package co.com.pragma.model.loan.gateways;

import co.com.pragma.model.loan.LoanRequest;
import reactor.core.publisher.Mono;

public interface LoanRequestRepository {
    Mono<LoanRequest> save(LoanRequest loanRequest);
    Mono<LoanRequest> findById(Long id);
    Mono<Boolean> existsByClientDocumentAndType(String clientDocumentId, LoanRequest.LoanType loanType);
}
