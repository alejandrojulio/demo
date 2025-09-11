package co.com.pragma.model.auth;

import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenValidationRequestDTO {
    
    private String token;
}
