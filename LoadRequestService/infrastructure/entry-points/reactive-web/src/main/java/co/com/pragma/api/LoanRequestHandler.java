package co.com.pragma.api;

import co.com.pragma.api.dto.ApiResponse;
import co.com.pragma.model.loan.LoanRequest;
import co.com.pragma.model.loan.LoanRequestDTO;
import co.com.pragma.usecase.loan.LoanRequestUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class LoanRequestHandler {
    
    private final LoanRequestUseCase loanRequestUseCase;

    public Mono<ServerResponse> createLoanRequest(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(LoanRequestDTO.class)
                .flatMap(loanRequestDTO -> loanRequestUseCase.createLoanRequest(loanRequestDTO))
                .flatMap(createdLoanRequest -> {
                    ApiResponse<LoanRequest> response = ApiResponse.success(
                            createdLoanRequest, 
                            "Solicitud de préstamo creada exitosamente"
                    );
                    return ServerResponse.status(HttpStatus.CREATED).bodyValue(response);
                })
                .onErrorResume(IllegalArgumentException.class, this::handleValidationError)
                .onErrorResume(Exception.class, this::handleGenericError);
    }

    private Mono<ServerResponse> handleValidationError(IllegalArgumentException error) {
        ApiResponse<String> response = ApiResponse.error(error.getMessage());
        
        return ServerResponse.status(HttpStatus.BAD_REQUEST)
                .bodyValue(response);
    }

    private Mono<ServerResponse> handleGenericError(Exception error) {
        ApiResponse<String> response = ApiResponse.error(
                "Ha ocurrido un error inesperado. Por favor, inténtelo más tarde."
        );
        
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .bodyValue(response);
    }
}
