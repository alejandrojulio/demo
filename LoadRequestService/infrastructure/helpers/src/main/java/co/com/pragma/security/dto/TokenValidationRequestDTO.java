package co.com.pragma.security.dto;

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
