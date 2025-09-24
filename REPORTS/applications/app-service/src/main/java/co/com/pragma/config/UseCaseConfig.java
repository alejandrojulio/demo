package co.com.pragma.config;

import co.com.pragma.usecase.report.GetLoanReportUseCase;
import co.com.pragma.usecase.report.ProcessLoanApprovedEventUseCase;
import co.com.pragma.usecase.report.gateways.EventPublisher;
import co.com.pragma.usecase.report.gateways.LoanReportRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {
    
    @Bean
    public GetLoanReportUseCase getLoanReportUseCase(LoanReportRepository loanReportRepository) {
        return new GetLoanReportUseCase(loanReportRepository);
    }
    
    @Bean
    public ProcessLoanApprovedEventUseCase processLoanApprovedEventUseCase(
            LoanReportRepository loanReportRepository,
            EventPublisher eventPublisher) {
        return new ProcessLoanApprovedEventUseCase(loanReportRepository, eventPublisher);
    }
}
