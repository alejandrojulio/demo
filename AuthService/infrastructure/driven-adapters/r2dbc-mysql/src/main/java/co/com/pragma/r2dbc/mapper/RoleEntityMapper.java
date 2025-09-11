package co.com.pragma.r2dbc.mapper;

import co.com.pragma.model.role.Role;
import co.com.pragma.r2dbc.entity.RoleEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Mapper para conversiones entre Role (domain) y RoleEntity (infrastructure)
 */
@Slf4j
public class RoleEntityMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private RoleEntityMapper() {
        // Constructor privado para evitar instanciación
    }

    /**
     * Convierte de RoleEntity a Role
     */
    public static Role fromEntity(RoleEntity entity) {
        if (entity == null) {
            return null;
        }

        Map<String, Object> permissions = null;
        if (entity.getPermissions() != null) {
            try {
                permissions = objectMapper.readValue(entity.getPermissions(), Map.class);
            } catch (JsonProcessingException e) {
                log.warn("Error parsing permissions JSON for role {}: {}", entity.getName(), e.getMessage());
                permissions = Map.of(); // Empty map as fallback
            }
        }

        return Role.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .permissions(permissions)
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Convierte de Role a RoleEntity
     */
    public static RoleEntity toEntity(Role role) {
        if (role == null) {
            return null;
        }

        var entity = new RoleEntity();
        entity.setId(role.getId());
        entity.setName(role.getName());
        entity.setDescription(role.getDescription());
        entity.setActive(role.getActive());
        entity.setCreatedAt(role.getCreatedAt());
        entity.setUpdatedAt(role.getUpdatedAt());

        // Convertir permissions Map a JSON String
        if (role.getPermissions() != null) {
            try {
                String permissionsJson = objectMapper.writeValueAsString(role.getPermissions());
                entity.setPermissions(permissionsJson);
            } catch (JsonProcessingException e) {
                log.warn("Error converting permissions to JSON for role {}: {}", role.getName(), e.getMessage());
                entity.setPermissions("{}"); // Empty JSON as fallback
            }
        }

        return entity;
    }
}
