package co.com.pragma.jwt;

import co.com.pragma.model.auth.gateways.JwtTokenGenerator;
import co.com.pragma.model.user.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenGeneratorImpl implements JwtTokenGenerator {

    private final JwtUtil jwtUtil;

    @Override
    public String generateToken(String userId, String email, UserRole role, String document) {
        log.debug("Generando token JWT para usuario: {} con rol: {}", email, role);
        return jwtUtil.generateToken(userId, email, role, document);
    }

    @Override
    public String extractUserId(String token) {
        try {
            return jwtUtil.extractUserId(token);
        } catch (Exception e) {
            log.warn("Error extrayendo userId del token: {}", e.getMessage());
            throw new RuntimeException("Token inválido", e);
        }
    }

    @Override
    public String extractEmail(String token) {
        try {
            return jwtUtil.extractEmail(token);
        } catch (Exception e) {
            log.warn("Error extrayendo email del token: {}", e.getMessage());
            throw new RuntimeException("Token inválido", e);
        }
    }

    @Override
    public UserRole extractRole(String token) {
        try {
            return jwtUtil.extractRole(token);
        } catch (Exception e) {
            log.warn("Error extrayendo rol del token: {}", e.getMessage());
            throw new RuntimeException("Token inválido", e);
        }
    }

    @Override
    public String extractDocument(String token) {
        try {
            return jwtUtil.extractDocument(token);
        } catch (Exception e) {
            log.warn("Error extrayendo documento del token: {}", e.getMessage());
            throw new RuntimeException("Token inválido", e);
        }
    }

    @Override
    public boolean isTokenValid(String token) {
        try {
            jwtUtil.validateToken(token);
            return !jwtUtil.isTokenExpired(token);
        } catch (Exception e) {
            log.debug("Token inválido: {}", e.getMessage());
            return false;
        }
    }
}
