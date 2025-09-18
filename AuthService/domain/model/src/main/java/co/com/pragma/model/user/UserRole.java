package co.com.pragma.model.user;

/**
 * Enumeración que define los roles de usuarios en el sistema CrediYa
 */
public enum UserRole {
    ADMIN("Administrador"),
    ADVISOR("Asesor"),
    CLIENT("Cliente");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Convierte un string a UserRole de manera case-insensitive
     */
    public static UserRole fromString(String role) {
        if (role == null) {
            return null;
        }
        
        for (UserRole userRole : values()) {
            if (userRole.name().equalsIgnoreCase(role) || 
                userRole.displayName.equalsIgnoreCase(role)) {
                return userRole;
            }
        }
        throw new IllegalArgumentException("Rol no válido: " + role);
    }
}
