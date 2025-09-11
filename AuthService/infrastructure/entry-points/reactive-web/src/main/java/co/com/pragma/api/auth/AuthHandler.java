package co.com.pragma.api.auth;

import co.com.pragma.api.dto.ApiResponse;
import co.com.pragma.model.auth.LoginRequestDTO;
import co.com.pragma.model.auth.LoginResponseDTO;
import co.com.pragma.model.auth.TokenValidationRequestDTO;
import co.com.pragma.model.auth.TokenValidationResponseDTO;
import co.com.pragma.usecase.auth.AuthenticationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuthHandler {

    private final AuthenticationUseCase authenticationUseCase;

    /**
     * Maneja el endpoint de login
     */
    public Mono<ServerResponse> login(ServerRequest request) {
        return request.bodyToMono(LoginRequestDTO.class)
                .flatMap(authenticationUseCase::login)
                .flatMap(this::buildLoginResponse)
                .onErrorResume(this::handleLoginError);
    }

    /**
     * Maneja el endpoint de validación de token
     */
    public Mono<ServerResponse> validateToken(ServerRequest request) {
        return request.bodyToMono(TokenValidationRequestDTO.class)
                .map(TokenValidationRequestDTO::getToken)
                .flatMap(authenticationUseCase::validateToken)
                .flatMap(this::buildValidationResponse)
                .onErrorResume(this::handleValidationError);
    }

    private Mono<ServerResponse> buildLoginResponse(LoginResponseDTO loginResponse) {
        if (loginResponse.isSuccess()) {
            ApiResponse<LoginResponseDTO> response = ApiResponse.<LoginResponseDTO>builder()
                    .success(true)
                    .message(loginResponse.getMessage())
                    .data(loginResponse)
                    .build();
                    
            return ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(response);
        } else {
            ApiResponse<String> response = ApiResponse.<String>builder()
                    .success(false)
                    .message(loginResponse.getMessage())
                    .data(null)
                    .build();
                    
            return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(response);
        }
    }

    private Mono<ServerResponse> buildValidationResponse(TokenValidationResponseDTO validationResponse) {
        if (validationResponse.isValid()) {
            ApiResponse<TokenValidationResponseDTO> response = ApiResponse.<TokenValidationResponseDTO>builder()
                    .success(true)
                    .message(validationResponse.getMessage())
                    .data(validationResponse)
                    .build();
                    
            return ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(response);
        } else {
            ApiResponse<String> response = ApiResponse.<String>builder()
                    .success(false)
                    .message(validationResponse.getMessage())
                    .data(validationResponse.getError())
                    .build();
                    
            return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(response);
        }
    }

    private Mono<ServerResponse> handleLoginError(Throwable error) {
        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(false)
                .message("Error interno del servidor durante la autenticación")
                .data(error.getMessage())
                .build();
                
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(response);
    }

    private Mono<ServerResponse> handleValidationError(Throwable error) {
        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(false)
                .message("Error interno del servidor durante la validación del token")
                .data(error.getMessage())
                .build();
                
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(response);
    }
}
