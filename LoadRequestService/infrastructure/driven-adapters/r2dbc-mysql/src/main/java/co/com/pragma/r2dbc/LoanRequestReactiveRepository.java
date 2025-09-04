package co.com.pragma.r2dbc;

import co.com.pragma.r2dbc.entity.LoanRequestEntity;
import co.com.pragma.r2dbc.entity.UserEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface LoanRequestReactiveRepository extends ReactiveCrudRepository<LoanRequestEntity, Long>, ReactiveQueryByExampleExecutor<LoanRequestEntity> {
    
    Mono<Boolean> existsByClientDocumentIdAndLoanType(String clientDocumentId, String loanType);
}
