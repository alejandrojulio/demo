package co.com.pragma.model.auth;

import co.com.pragma.model.user.UserRole;
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
