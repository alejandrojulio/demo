package co.com.pragma.config;

import co.com.pragma.model.loan.gateways.LoanApplicationLogger;
import co.com.pragma.model.loan.gateways.LoanRequestRepository;
import co.com.pragma.model.notification.gateways.NotificationGateway;
import co.com.pragma.model.loantype.gateways.LoanTypeRepository;
import co.com.pragma.model.debtcapacity.gateways.DebtCapacityGateway;
import co.com.pragma.model.user.gateways.UserDataGateway;
import co.com.pragma.usecase.loan.ListLoanRequestsForReviewUseCase;
import co.com.pragma.usecase.loan.LoanRequestUseCase;
import co.com.pragma.usecase.loan.ProcessLoanDecisionUseCase;
import co.com.pragma.usecase.loan.UpdateLoanStatusUseCase;
import co.com.pragma.usecase.loan.gateways.LoanEventPublisher;
import co.com.pragma.usecase.notification.NotificationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCasesConfig {

    @Bean
    public LoanRequestUseCase loanRequestUseCase(
            LoanRequestRepository loanRequestRepository,
            LoanApplicationLogger logger,
            LoanTypeRepository loanTypeRepository,
            DebtCapacityGateway debtCapacityGateway) {
        return new LoanRequestUseCase(loanRequestRepository, logger, loanTypeRepository, debtCapacityGateway);
    }

    @Bean
    public ListLoanRequestsForReviewUseCase listLoanRequestsForReviewUseCase(
            LoanRequestRepository loanRequestRepository,
            LoanApplicationLogger logger,
            UserDataGateway userDataGateway) {
        return new ListLoanRequestsForReviewUseCase(loanRequestRepository, logger, userDataGateway);
    }

    @Bean
    public NotificationService notificationService(
            NotificationGateway notificationGateway,
            LoanApplicationLogger logger,
            LoanRequestRepository loanRequestRepository,
            UserDataGateway userDataGateway) {
        return new NotificationService(notificationGateway, logger, loanRequestRepository, userDataGateway);
    }

    @Bean
    public ProcessLoanDecisionUseCase processLoanDecisionUseCase(
            LoanRequestRepository loanRequestRepository,
            LoanApplicationLogger logger,
            NotificationService notificationService,
            LoanEventPublisher loanEventPublisher) {
        return new ProcessLoanDecisionUseCase(loanRequestRepository, logger, notificationService, loanEventPublisher);
    }

    @Bean
    public UpdateLoanStatusUseCase updateLoanStatusUseCase(
            LoanRequestRepository loanRequestRepository,
            LoanApplicationLogger logger,
            LoanEventPublisher loanEventPublisher) {
        return new UpdateLoanStatusUseCase(loanRequestRepository, logger, loanEventPublisher);
    }
}
