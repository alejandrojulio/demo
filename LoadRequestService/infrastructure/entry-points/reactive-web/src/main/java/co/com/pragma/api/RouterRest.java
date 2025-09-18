package co.com.pragma.api;

import co.com.pragma.model.common.Messages;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.PUT;
import static org.springframework.web.reactive.function.server.RequestPredicates.PATCH;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {
    @Bean
    public RouterFunction<ServerResponse> routerFunction(LoanRequestHandler loanRequestHandler) {
        return route(POST(Messages.Endpoints.LOAN_REQUEST_BASE), loanRequestHandler::createLoanRequest)
                .andRoute(PUT(Messages.Endpoints.LOAN_REQUEST_BY_ID), loanRequestHandler::updateLoanRequest)
                .andRoute(GET(Messages.Endpoints.LOAN_REQUEST_BASE), loanRequestHandler::getSolicitudesForReview)
                .andRoute(PUT(Messages.Endpoints.LOAN_REQUEST_BASE), loanRequestHandler::processLoanDecision)
                .andRoute(PATCH(Messages.Endpoints.LOAN_REQUEST_BY_ID + "/status"), loanRequestHandler::updateLoanStatus);
    }
}
