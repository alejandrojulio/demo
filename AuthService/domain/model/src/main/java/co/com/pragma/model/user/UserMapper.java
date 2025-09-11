package co.com.pragma.model.user;

import co.com.pragma.model.role.RoleMapper;

public class UserMapper {

    private UserMapper() {
        // Constructor privado para evitar instanciación
    }

    public static User fromDTO(UserDTO userDTO) {
        return User.builder()
                .document(userDTO.getDocument())
                .firstName(userDTO.getFirstName())
                .lastName(userDTO.getLastName())
                .birthDate(userDTO.getBirthDate())
                .address(userDTO.getAddress())
                .phone(userDTO.getPhone())
                .email(userDTO.getEmail())
                .baseSalary(userDTO.getBaseSalary())
                .password(userDTO.getPassword()) // Contraseña sin encriptar (se encriptará en el Use Case)
                .role(userDTO.getRole() != null ? userDTO.getRole() : UserRole.CLIENTE)
                .isActive(true) // Por defecto los usuarios nuevos están activos
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
    }

    public static UserResponseDTO toResponseDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .document(user.getDocument())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .birthDate(user.getBirthDate())
                .address(user.getAddress())
                .phone(user.getPhone())
                .email(user.getEmail())
                .baseSalary(user.getBaseSalary())
                .role(user.getRole())
                .isActive(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .fullName(user.getFullName())
                .isAdult(user.isAdult())
                .build();
    }

    public static User enrichUserData(User user) {
        return user.toBuilder()
                .document(user.getDocument() != null ? user.getDocument().trim() : null)
                .email(user.getEmail() != null ? user.getEmail().toLowerCase().trim() : null)
                .firstName(user.getFirstName() != null ? user.getFirstName().trim() : null)
                .lastName(user.getLastName() != null ? user.getLastName().trim() : null)
                .address(user.getAddress() != null ? user.getAddress().trim() : null)
                .phone(user.getPhone() != null ? user.getPhone().trim() : null)
                .role(user.getRole() != null ? user.getRole() : UserRole.CLIENTE) // Default role
                .build();
    }
    
}
