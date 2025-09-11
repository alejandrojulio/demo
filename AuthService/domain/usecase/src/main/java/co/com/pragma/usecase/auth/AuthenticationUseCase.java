package co.com.pragma.usecase.auth;

import co.com.pragma.model.auth.LoginRequestDTO;
import co.com.pragma.model.auth.LoginResponseDTO;
import co.com.pragma.model.auth.TokenValidationResponseDTO;
import co.com.pragma.model.auth.gateways.JwtTokenGenerator;
import co.com.pragma.model.auth.gateways.PasswordEncoder;
import co.com.pragma.model.common.MessageFormatter;
import co.com.pragma.model.common.Messages;
import co.com.pragma.model.user.User;
import co.com.pragma.model.user.UserRole;
import co.com.pragma.model.user.gateways.ApplicationLogger;
import co.com.pragma.model.user.gateways.UserRepository;
import reactor.core.publisher.Mono;

public class AuthenticationUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenGenerator jwtTokenGenerator;
    private final ApplicationLogger logger;

    public AuthenticationUseCase(UserRepository userRepository, 
                               PasswordEncoder passwordEncoder,
                               JwtTokenGenerator jwtTokenGenerator,
                               ApplicationLogger logger) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenGenerator = jwtTokenGenerator;
        this.logger = logger;
    }

    /**
     * Autentica un usuario y genera un token JWT
     */
    public Mono<LoginResponseDTO> login(LoginRequestDTO loginRequest) {
        logger.info(MessageFormatter.format(Messages.LOG_AUTH_STARTED, loginRequest.getEmail()));
        
        return userRepository.findByEmail(loginRequest.getEmail().toLowerCase().trim())
                .cast(User.class)
                .flatMap(user -> validateCredentials(user, loginRequest.getPassword())
                        .flatMap(validUser -> generateAuthResponse(validUser)))
                .switchIfEmpty(Mono.fromCallable(() -> {
                    logger.warn(MessageFormatter.format(Messages.LOG_AUTH_FAILED_USER_NOT_FOUND, loginRequest.getEmail()));
                    return LoginResponseDTO.builder()
                            .success(false)
                            .message(Messages.AUTH_INVALID_CREDENTIALS)
                            .build();
                }))
                .onErrorResume(Exception.class, error -> {
                    logger.error(MessageFormatter.format(Messages.LOG_AUTH_ERROR, loginRequest.getEmail(), error.getMessage()), error);
                    return Mono.just(LoginResponseDTO.builder()
                            .success(false)
                            .message(Messages.AUTH_INTERNAL_ERROR)
                            .build());
                });
    }

    /**
     * Valida un token JWT y devuelve la información del usuario
     */
    public Mono<TokenValidationResponseDTO> validateToken(String token) {
        logger.info(Messages.LOG_TOKEN_VALIDATION_STARTED);
        
        try {
            if (!jwtTokenGenerator.isTokenValid(token)) {
                logger.warn(Messages.LOG_TOKEN_INVALID);
                return Mono.just(TokenValidationResponseDTO.builder()
                        .valid(false)
                        .error(Messages.AUTH_INVALID_TOKEN)
                        .message(Messages.AUTH_ACCESS_DENIED)
                        .build());
            }
            
            String userId = jwtTokenGenerator.extractUserId(token);
            String email = jwtTokenGenerator.extractEmail(token);
            UserRole role = jwtTokenGenerator.extractRole(token);
            String document = jwtTokenGenerator.extractDocument(token);
            
            logger.info(MessageFormatter.format(Messages.LOG_TOKEN_VALID, email, role));
            
            return Mono.just(TokenValidationResponseDTO.builder()
                    .valid(true)
                    .id(userId)
                    .email(email)
                    .role(role)
                    .document(document)
                    .message(Messages.AUTH_VALID_TOKEN)
                    .build());
                    
        } catch (RuntimeException e) {
            logger.warn(MessageFormatter.format(Messages.LOG_TOKEN_EXTRACT_ERROR, "información", e.getMessage()));
            return Mono.just(TokenValidationResponseDTO.builder()
                    .valid(false)
                    .error(Messages.AUTH_INVALID_TOKEN)
                    .message(Messages.AUTH_ACCESS_DENIED)
                    .build());
        } catch (Exception e) {
            logger.error(MessageFormatter.format(Messages.LOG_TOKEN_ERROR, e.getMessage()), e);
            return Mono.just(TokenValidationResponseDTO.builder()
                    .valid(false)
                    .error(Messages.AUTH_INTERNAL_ERROR)
                    .message(Messages.AUTH_VALIDATION_ERROR)
                    .build());
        }
    }

    private Mono<User> validateCredentials(User user, String password) {
        return Mono.fromCallable(() -> {
            if (passwordEncoder.matches(password, user.getPassword())) {
                logger.info(MessageFormatter.format(Messages.LOG_AUTH_SUCCESS, user.getEmail()));
                return user;
            } else {
                logger.warn(MessageFormatter.format(Messages.LOG_AUTH_FAILED_WRONG_PASSWORD, user.getEmail()));
                throw new RuntimeException(Messages.AUTH_INVALID_CREDENTIALS);
            }
        });
    }

    private Mono<LoginResponseDTO> generateAuthResponse(User user) {
        return Mono.fromCallable(() -> {
            String token = jwtTokenGenerator.generateToken(user.getId(), user.getEmail(), user.getRole(), user.getDocument());
            
            logger.info(MessageFormatter.format(Messages.LOG_TOKEN_GENERATED, user.getEmail(), user.getRole()));
            
            return LoginResponseDTO.builder()
                    .success(true)
                    .token(token)
                    .id(user.getId())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .message(Messages.AUTH_SUCCESS)
                    .build();
        });
    }
}
