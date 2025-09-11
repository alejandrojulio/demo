package co.com.pragma.model.auth.gateways;

public interface PasswordEncoder {
    /**
     * Encripta una contraseña en texto plano
     */
    String encode(String rawPassword);
    
    /**
     * Verifica si una contraseña en texto plano coincide con una contraseña encriptada
     */
    boolean matches(String rawPassword, String encodedPassword);
}
