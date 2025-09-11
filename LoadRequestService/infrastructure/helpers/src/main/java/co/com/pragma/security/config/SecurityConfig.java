package co.com.pragma.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class SecurityConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    /**
     * El filtro JWT se registra automáticamente como WebFilter al ser un @Component
     * Spring Boot detecta automáticamente los WebFilter y los aplica en el orden correcto
     * 
     * No necesitamos inyectar JwtAuthenticationFilter aquí para evitar dependencias circulares
     */
}
