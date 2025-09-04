package co.com.pragma.api;

import co.com.pragma.usecase.user.UserUseCase;
import co.com.pragma.model.user.UserDTO;
import co.com.pragma.model.user.UserResponseDTO;
import co.com.pragma.api.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class Handler {
    private final UserUseCase userUseCase;

    public Mono<ServerResponse> listenGETUseCase(ServerRequest serverRequest) {
        String userId = serverRequest.pathVariable("id");

        return userUseCase.getuser(userId.toString())
                .flatMap(user -> {
                    ApiResponse<UserResponseDTO> response = ApiResponse.success(
                            user, 
                            "Usuario encontrado exitosamente"
                    );
                    return ServerResponse.ok().bodyValue(response);
                })
                .switchIfEmpty(ServerResponse.notFound().build())
                .onErrorResume(throwable -> {
                    ApiResponse<String> errorResponse = ApiResponse.error(
                            "Error al buscar el usuario: " + throwable.getMessage()
                    );
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .bodyValue(errorResponse);
                });
    }

    public Mono<ServerResponse> listenPOSTUseCase(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(UserDTO.class)
                .flatMap(userDTO -> userUseCase.createUser(userDTO))
                .flatMap(createdUser -> {
                    ApiResponse<UserResponseDTO> response = ApiResponse.success(
                            createdUser, 
                            "Usuario creado exitosamente"
                    );
                    return ServerResponse.status(HttpStatus.CREATED).bodyValue(response);
                })
                .onErrorResume(throwable -> {
                    ApiResponse<String> errorResponse = ApiResponse.error(
                            "Error al crear el usuario: " + throwable.getMessage()
                    );
                    return ServerResponse.status(HttpStatus.BAD_REQUEST)
                            .bodyValue(errorResponse);
                });
    }
}
