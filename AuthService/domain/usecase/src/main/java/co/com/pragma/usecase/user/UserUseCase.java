package co.com.pragma.usecase.user;

import co.com.pragma.model.user.gateways.ApplicationLogger;
import lombok.RequiredArgsConstructor;
import co.com.pragma.model.user.User;
import co.com.pragma.model.user.gateways.UserRepository;
import reactor.core.publisher.Mono;

public class UserUseCase {

    private final UserRepository userRepository;
    private final ApplicationLogger logger;

    public UserUseCase(UserRepository userRepository, ApplicationLogger logger) {
        this.userRepository = userRepository;
        this.logger = logger;
    }

    public Mono<User> getuser(String id){
        return userRepository.findById(id);
    }

    public Mono<User> createUser(User userToCreate) {
        logger.info("Starting user creation process for email: {}", userToCreate.getEmail());

        return Mono.just(userToCreate)
                .flatMap(this::ensureEmailIsUnique)
                .map(this::enrichUserData)
                .flatMap(userRepository::save)
                .doOnSuccess(savedUser -> logger.info("User created successfully with ID: {}", savedUser.getId()));
    }

    private Mono<User> ensureEmailIsUnique(User user) {
        return userRepository.existsByEmail(user.getEmail().toLowerCase())
                .flatMap(emailExists -> {
                    if (emailExists) {
                        return Mono.error(new RuntimeException("El email ya está registrado: " + user.getEmail()));
                    }
                    return Mono.just(user);
                });
    }

    private User enrichUserData(User user) {
        return user.toBuilder()
                .email(user.getEmail().toLowerCase().trim())
                .firstName(user.getFirstName().trim())
                .lastName(user.getLastName().trim())
                .build();
    }
}
