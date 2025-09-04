package co.com.pragma.model.user;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {
    
    private String id;
    private String document;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String address;
    private String phone;
    private String email;
    private BigDecimal baseSalary;
    private String fullName;
    private boolean isAdult;
}
