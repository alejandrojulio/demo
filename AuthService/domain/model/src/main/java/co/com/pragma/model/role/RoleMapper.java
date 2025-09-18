package co.com.pragma.model.role;

import co.com.pragma.model.user.UserRole;


public class RoleMapper {
    
    /**
     * Convierte un string de rol de BD directamente a UserRole
     * Ahora que tenemos una columna role directa, este es el método principal
     */
    public static UserRole roleNameToUserRole(String roleName) {
        if (roleName == null) {
            return UserRole.CLIENT;
        }
        
        return switch (roleName.toUpperCase()) {
            case "ADMIN", "ADMINISTRADOR" -> UserRole.ADMIN;
            case "ADVISOR", "ASESOR" -> UserRole.ADVISOR;
            case "CLIENT", "CLIENTE" -> UserRole.CLIENT;
            default -> UserRole.CLIENT;
        };
    }

    /**
     * Convierte UserRole a string para la BD
     */
    public static String userRoleToRoleName(UserRole userRole) {
        if (userRole == null) {
            return "CLIENT";
        }
        
        return userRole.name(); // Devuelve ADMIN, ADVISOR, o CLIENT
    }

    // Métodos legacy mantenidos para compatibilidad pero ya no se usan
    @Deprecated
    public static UserRole roleIdToUserRole(Integer roleId) {
        if (roleId == null) {
            return UserRole.CLIENT;
        }
        
        return switch (roleId) {
            case 1 -> UserRole.ADMIN;
            case 2 -> UserRole.ADVISOR;
            case 3 -> UserRole.CLIENT;
            default -> UserRole.CLIENT;
        };
    }
    
    @Deprecated
    public static Integer userRoleToRoleId(UserRole userRole) {
        if (userRole == null) {
            return 3; // Default: CLIENT
        }
        
        return switch (userRole) {
            case ADMIN -> 1;
            case ADVISOR -> 2;
            case CLIENT -> 3;
        };
    }
}
