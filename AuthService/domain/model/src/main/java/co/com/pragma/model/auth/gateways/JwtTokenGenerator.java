package co.com.pragma.model.auth.gateways;

import co.com.pragma.model.user.UserRole;

/**
 * Gateway para generación y validación de tokens JWT
 */
public interface JwtTokenGenerator {
    
    /**
     * Genera un token JWT para el usuario autenticado
     */
    String generateToken(String userId, String email, UserRole role, String document);
    
    /**
     * Valida un token JWT y extrae el ID del usuario
     */
    String extractUserId(String token);
    
    /**
     * Valida un token JWT y extrae el email del usuario
     */
    String extractEmail(String token);
    
    /**
     * Valida un token JWT y extrae el rol del usuario
     */
    UserRole extractRole(String token);
    
    /**
     * Valida un token JWT y extrae el documento del usuario
     */
    String extractDocument(String token);
    
    /**
     * Verifica si el token es válido
     */
    boolean isTokenValid(String token);
}
