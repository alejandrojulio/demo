package co.com.pragma.api.config;

import co.com.pragma.model.auth.gateways.JwtTokenGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtTokenGenerator jwtTokenGenerator;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = authentication.getCredentials().toString();
        
        return Mono.fromCallable(() -> {
            try {
                if (!jwtTokenGenerator.isTokenValid(token)) {
                    log.warn("Token JWT inválido en AuthService");
                    throw new RuntimeException("Token inválido");
                }

                String userId = jwtTokenGenerator.extractUserId(token);
                String email = jwtTokenGenerator.extractEmail(token);
                String role = jwtTokenGenerator.extractRole(token).toString();

                log.info("Token válido para usuario: {} con rol: {}", email, role);

                // Crear las autoridades basadas en el rol
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
                
                return new UsernamePasswordAuthenticationToken(
                        email, 
                        null, 
                        Collections.singletonList(authority)
                );
                
            } catch (Exception e) {
                log.warn("Error validando token en AuthService: {}", e.getMessage());
                throw new RuntimeException("Error de autenticación", e);
            }
        });
    }
}
