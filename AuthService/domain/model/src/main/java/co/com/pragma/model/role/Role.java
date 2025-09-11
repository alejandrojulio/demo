package co.com.pragma.model.role;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Role {
    private Integer id;
    private String name;
    private String description;
    private Map<String, Object> permissions;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Verifica si el rol tiene un permiso específico
     */
    public boolean hasPermission(String resource, String action) {
        if (permissions == null || !permissions.containsKey(resource)) {
            return false;
        }
        
        Object resourcePermissions = permissions.get(resource);
        if (resourcePermissions instanceof java.util.List) {
            @SuppressWarnings("unchecked")
            java.util.List<String> permissionsList = (java.util.List<String>) resourcePermissions;
            return permissionsList.contains(action);
        }
        
        return false;
    }
    
    /**
     * Convierte el enum UserRole a nombre de rol
     */
    public static String getRoleName(co.com.pragma.model.user.UserRole userRole) {
        return userRole.name();
    }
}
