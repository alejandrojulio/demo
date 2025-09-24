package co.com.pragma.api;

import co.com.pragma.model.common.Messages;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {
    
    @Bean
    public RouterFunction<ServerResponse> routerFunction(ReportsHandler reportsHandler) {
        return route(GET(Messages.Endpoints.REPORTS_BASE), reportsHandler::getApprovedLoansReport)
                .andRoute(GET(Messages.Endpoints.LOAN_COUNTER), reportsHandler::getApprovedLoansReport)
                .andRoute(GET("/health"), reportsHandler::getHealthCheck);
    }
}
