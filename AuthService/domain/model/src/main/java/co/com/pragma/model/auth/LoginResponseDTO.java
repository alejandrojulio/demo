package co.com.pragma.model.auth;

import co.com.pragma.model.user.UserRole;
import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDTO {
    
    private String token;
    private String id;
    private String email;
    private UserRole role;
    private String message;
    private boolean success;
}
