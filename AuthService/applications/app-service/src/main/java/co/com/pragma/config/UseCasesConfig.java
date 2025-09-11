package co.com.pragma.config;

import co.com.pragma.model.auth.gateways.JwtTokenGenerator;
import co.com.pragma.model.auth.gateways.PasswordEncoder;
import co.com.pragma.model.user.gateways.ApplicationLogger;
import co.com.pragma.model.user.gateways.UserRepository;
import co.com.pragma.usecase.auth.AuthenticationUseCase;
import co.com.pragma.usecase.user.UserUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCasesConfig {

    @Bean
    public AuthenticationUseCase authenticationUseCase(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenGenerator jwtTokenGenerator,
            ApplicationLogger logger) {
        return new AuthenticationUseCase(userRepository, passwordEncoder, jwtTokenGenerator, logger);
    }

    @Bean
    public UserUseCase userUseCase(
            UserRepository userRepository,
            ApplicationLogger logger,
            PasswordEncoder passwordEncoder) {
        return new UserUseCase(userRepository, logger, passwordEncoder);
    }
}
