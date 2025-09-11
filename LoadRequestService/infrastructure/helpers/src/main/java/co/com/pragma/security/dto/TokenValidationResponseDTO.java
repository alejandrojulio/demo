package co.com.pragma.security.dto;

import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenValidationResponseDTO {
    private boolean valid;
    private String id;
    private String email;
    private UserRole role;
    private String document;
    private String message;
    private String error;
}

/**
 * Enum que debe coincidir exactamente con el del AuthService
 */
enum UserRole {
    ADMINISTRADOR("Administrador"),
    ASESOR("Asesor"),
    CLIENTE("Cliente");
    
    private final String displayName;
    
    UserRole(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    // Sobrescribir toString para que coincida con AuthService
    @Override
    public String toString() {
        return this.name(); // Devuelve ADMINISTRADOR, ASESOR, CLIENTE
    }
}
