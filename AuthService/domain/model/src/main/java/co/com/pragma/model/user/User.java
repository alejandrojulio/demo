package co.com.pragma.model.user;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class User {
    private String id;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String address;
    private String phone;
    private String email;
    private BigDecimal baseSalary;

    public boolean isAdult() {
        return birthDate != null && LocalDate.now().minusYears(18).isAfter(birthDate);
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
