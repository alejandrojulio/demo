// Script para generar hash correcto de password123
// Ejecutar este código Java temporal para generar el hash

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestPasswordHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "password123";
        String hash = encoder.encode(password);
        
        System.out.println("Password: " + password);
        System.out.println("Hash: " + hash);
        
        // Verificar que funcione
        boolean matches = encoder.matches(password, hash);
        System.out.println("Verification: " + matches);
        
        // También probar con el hash que tenemos en BD
        String existingHash = "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi";
        boolean existingMatches = encoder.matches(password, existingHash);
        System.out.println("Existing hash verification: " + existingMatches);
    }
}
