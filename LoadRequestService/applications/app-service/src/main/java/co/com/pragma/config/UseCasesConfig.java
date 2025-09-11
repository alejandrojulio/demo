package co.com.pragma.config;

import co.com.pragma.model.loan.gateways.LoanApplicationLogger;
import co.com.pragma.model.loan.gateways.LoanRequestRepository;
import co.com.pragma.model.notification.gateways.NotificationGateway;
import co.com.pragma.usecase.loan.ListLoanRequestsForReviewUseCase;
import co.com.pragma.usecase.loan.LoanRequestUseCase;
import co.com.pragma.usecase.loan.ProcessLoanDecisionUseCase;
import co.com.pragma.usecase.notification.NotificationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCasesConfig {

    @Bean
    public LoanRequestUseCase loanRequestUseCase(
            LoanRequestRepository loanRequestRepository,
            LoanApplicationLogger logger) {
        return new LoanRequestUseCase(loanRequestRepository, logger);
    }

    @Bean
    public ListLoanRequestsForReviewUseCase listLoanRequestsForReviewUseCase(
            LoanRequestRepository loanRequestRepository,
            LoanApplicationLogger logger) {
        return new ListLoanRequestsForReviewUseCase(loanRequestRepository, logger);
    }

    @Bean
    public NotificationService notificationService(
            NotificationGateway notificationGateway,
            LoanApplicationLogger logger,
            LoanRequestRepository loanRequestRepository) {
        return new NotificationService(notificationGateway, logger, loanRequestRepository);
    }

    @Bean
    public ProcessLoanDecisionUseCase processLoanDecisionUseCase(
            LoanRequestRepository loanRequestRepository,
            LoanApplicationLogger logger,
            NotificationService notificationService) {
        return new ProcessLoanDecisionUseCase(loanRequestRepository, logger, notificationService);
    }
}
