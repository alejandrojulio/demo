package co.com.pragma.r2dbc.mapper;

import co.com.pragma.model.role.RoleMapper;
import co.com.pragma.model.user.User;
import co.com.pragma.r2dbc.entity.UserEntity;

/**
 * Mapper para conversiones entre User (domain) y UserEntity (infrastructure)
 */
public class UserEntityMapper {

    private UserEntityMapper() {
        // Constructor privado para evitar instanciación
    }

    /**
     * Convierte de UserEntity a User (considerando role_id)
     */
    public static User fromEntity(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return User.builder()
                .id(entity.getId())
                .document(entity.getDocument())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .birthDate(entity.getBirthDate())
                .address(entity.getAddress())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .baseSalary(entity.getBaseSalary())
                .password(entity.getPassword())
                .role(RoleMapper.roleNameToUserRole(entity.getRole()))
                .isActive(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    
    /**
     * Convierte de User a UserEntity (considerando role_id)
     */
    public static UserEntity toEntity(User user) {
        if (user == null) {
            return null;
        }
        
        var entity = new UserEntity();
        entity.setId(user.getId());
        entity.setDocument(user.getDocument());
        entity.setFirstName(user.getFirstName());
        entity.setLastName(user.getLastName());
        entity.setBirthDate(user.getBirthDate());
        entity.setAddress(user.getAddress());
        entity.setPhone(user.getPhone());
        entity.setEmail(user.getEmail());
        entity.setBaseSalary(user.getBaseSalary());
        entity.setPassword(user.getPassword());
        entity.setRole(user.getRole().name());
        entity.setActive(user.isActive());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());
        return entity;
    }
}
