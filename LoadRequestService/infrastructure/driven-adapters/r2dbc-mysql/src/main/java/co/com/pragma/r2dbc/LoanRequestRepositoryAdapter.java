package co.com.pragma.r2dbc;

import co.com.pragma.model.loan.LoanRequest;
import co.com.pragma.model.loan.gateways.LoanRequestRepository;
import co.com.pragma.r2dbc.entity.LoanRequestEntity;
import co.com.pragma.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class LoanRequestRepositoryAdapter extends ReactiveAdapterOperations<
        LoanRequest,
        LoanRequestEntity,
        Long,
        LoanRequestReactiveRepository>
        implements LoanRequestRepository {

    public LoanRequestRepositoryAdapter(LoanRequestReactiveRepository repository, ObjectMapper mapper) {
        super(repository, mapper, d -> mapper.map(d, LoanRequest.class));
    }

    @Override
    public Mono<LoanRequest> save(LoanRequest loanRequest) {
        return super.save(loanRequest);
    }

    @Override
    public Mono<Boolean> existsByClientDocumentAndType(String clientDocumentId, LoanRequest.LoanType loanType) {
        return repository.existsByClientDocumentIdAndLoanType(clientDocumentId, loanType.name());
    }
}
