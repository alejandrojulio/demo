package co.com.pragma.model.role;

import co.com.pragma.model.user.UserRole;


public class RoleMapper {
    
    public static UserRole roleIdToUserRole(Integer roleId) {
        if (roleId == null) {
            return UserRole.CLIENTE;
        }
        
        return switch (roleId) {
            case 1 -> UserRole.ADMINISTRADOR;
            case 2 -> UserRole.ASESOR;
            case 3 -> UserRole.CLIENTE;
            default -> UserRole.CLIENTE;
        };
    }
    
    public static Integer userRoleToRoleId(UserRole userRole) {
        if (userRole == null) {
            return 3; // Default: CLIENTE
        }
        
        return switch (userRole) {
            case ADMINISTRADOR -> 1;
            case ASESOR -> 2;
            case CLIENTE -> 3;
        };
    }

    public static UserRole roleNameToUserRole(String roleName) {
        if (roleName == null) {
            return UserRole.CLIENTE;
        }
        
        return switch (roleName.toUpperCase()) {
            case "ADMINISTRADOR" -> UserRole.ADMINISTRADOR;
            case "ASESOR" -> UserRole.ASESOR;
            case "CLIENTE" -> UserRole.CLIENTE;
            default -> UserRole.CLIENTE;
        };
    }
}
