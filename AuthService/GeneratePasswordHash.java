// Ejecutar este código para generar el hash correcto
// javac -cp "path-to-spring-security.jar" GeneratePasswordHash.java && java GeneratePasswordHash

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GeneratePasswordHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "password123";
        
        // Generar varios hashes para verificar
        for (int i = 0; i < 3; i++) {
            String hash = encoder.encode(password);
            boolean matches = encoder.matches(password, hash);
            System.out.println("Hash " + (i+1) + ": " + hash);
            System.out.println("Matches: " + matches);
            System.out.println("---");
        }
    }
}
