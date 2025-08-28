package co.com.pragma.api;

import co.com.pragma.usecase.user.UserUseCase;
import co.com.pragma.model.user.User;
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
                .flatMap(user -> ServerResponse.ok().bodyValue(user))
                .switchIfEmpty(ServerResponse.notFound().build())
                .onErrorResume(throwable -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .bodyValue("Error al buscar el usuario: " + throwable.getMessage()));
    }

    public Mono<ServerResponse> listenPOSTUseCase(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(User.class)
                .flatMap(userToCreate -> userUseCase.createUser(userToCreate))
                .flatMap(createdUser -> ServerResponse.status(HttpStatus.CREATED)
                        .bodyValue(createdUser))
                .onErrorResume(throwable -> ServerResponse.status(HttpStatus.BAD_REQUEST)
                        .bodyValue("Error al crear el usuario: " + throwable.getMessage()));
    }
}
