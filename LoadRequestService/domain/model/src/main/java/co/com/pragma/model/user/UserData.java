package co.com.pragma.model.user;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Modelo de dominio para datos de usuario obtenidos desde AuthService
 * Contiene solo la información necesaria para el dominio de préstamos
 */
@Data
@Builder
public class UserData {
    private Long id;
    private String document;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private BigDecimal baseSalary;
    private Boolean isActive;
    private Boolean emailVerified;
    private LocalDateTime createdAt;
    
    /**
     * Obtiene el nombre completo del usuario
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    /**
     * Verifica si el usuario está activo y tiene email verificado
     */
    public boolean isValidForLoan() {
        return Boolean.TRUE.equals(isActive) && Boolean.TRUE.equals(emailVerified);
    }
    
    /**
     * Verifica si el usuario tiene un salario válido para préstamos
     */
    public boolean hasValidSalary() {
        return baseSalary != null && baseSalary.compareTo(BigDecimal.ZERO) > 0;
    }
}
