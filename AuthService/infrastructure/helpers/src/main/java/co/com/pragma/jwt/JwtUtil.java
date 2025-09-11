package co.com.pragma.jwt;

import co.com.pragma.model.common.Messages;
import co.com.pragma.model.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expirationTimeInHours;

    public JwtUtil(@Value("${jwt.secret:" + Messages.Config.DEFAULT_JWT_SECRET + "}") String secret,
                   @Value("${jwt.expiration:" + Messages.Config.DEFAULT_JWT_EXPIRATION_HOURS + "}") long expirationTimeInHours) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationTimeInHours = expirationTimeInHours;
    }

    /**
     * Genera un token JWT para el usuario autenticado
     */
    public String generateToken(String userId, String email, UserRole role, String document) {
        Instant now = Instant.now();
        Instant expiration = now.plus(expirationTimeInHours, ChronoUnit.HOURS);

        return Jwts.builder()
                .setSubject(userId)
                .claim(Messages.JwtClaims.EMAIL, email)
                .claim(Messages.JwtClaims.ROLE, role.name())
                .claim(Messages.JwtClaims.DOCUMENT, document)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Valida un token JWT y extrae las claims
     */
    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtException(Messages.ERROR_JWT_INVALID.replace("{0}", e.getMessage()));
        }
    }

    /**
     * Extrae el ID del usuario del token
     */
    public String extractUserId(String token) {
        Claims claims = validateToken(token);
        return claims.getSubject();
    }

    /**
     * Extrae el email del usuario del token
     */
    public String extractEmail(String token) {
        Claims claims = validateToken(token);
        return claims.get(Messages.JwtClaims.EMAIL, String.class);
    }

    /**
     * Extrae el rol del usuario del token
     */
    public UserRole extractRole(String token) {
        Claims claims = validateToken(token);
        String roleStr = claims.get(Messages.JwtClaims.ROLE, String.class);
        return UserRole.valueOf(roleStr);
    }

    /**
     * Extrae el documento del usuario del token
     */
    public String extractDocument(String token) {
        Claims claims = validateToken(token);
        return claims.get(Messages.JwtClaims.DOCUMENT, String.class);
    }

    /**
     * Verifica si el token ha expirado
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = validateToken(token);
            return claims.getExpiration().before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }
}
