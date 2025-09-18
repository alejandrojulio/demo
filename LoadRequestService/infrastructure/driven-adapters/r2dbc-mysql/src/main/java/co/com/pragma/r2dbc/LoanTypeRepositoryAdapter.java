package co.com.pragma.r2dbc;

import co.com.pragma.model.loantype.LoanType;
import co.com.pragma.model.loantype.gateways.LoanTypeRepository;
import co.com.pragma.r2dbc.entity.LoanTypeEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Implementación del repositorio de tipos de préstamo usando R2DBC
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class LoanTypeRepositoryAdapter implements LoanTypeRepository {
    
    private final R2dbcEntityTemplate template;
    
    @Override
    public Mono<LoanType> findByTypeCode(LoanType.LoanTypeCode typeCode) {
        log.debug("Buscando tipo de préstamo por código: {}", typeCode);
        
        String sql = """
            SELECT * FROM loan_types 
            WHERE type_code = :type_code AND is_active = true
            """;
            
        return template.getDatabaseClient()
                .sql(sql)
                .bind("type_code", typeCode.getCode())
                .fetch()
                .first()
                .map(this::mapRowToEntity)
                .map(this::mapToDomain)
                .doOnNext(loanType -> log.debug("Tipo de préstamo encontrado: {}", loanType.getDisplayName()))
                .doOnError(error -> log.error("Error buscando tipo de préstamo {}: {}", typeCode, error.getMessage()));
    }
    
    @Override
    public Flux<LoanType> findAllActive() {
        log.debug("Obteniendo todos los tipos de préstamo activos");
        
        String sql = """
            SELECT * FROM loan_types 
            WHERE is_active = true 
            ORDER BY type_code
            """;
        
        return template.getDatabaseClient()
                .sql(sql)
                .fetch()
                .all()
                .map(this::mapRowToEntity)
                .map(this::mapToDomain)
                .doOnNext(loanType -> log.debug("Tipo de préstamo activo: {}", loanType.getDisplayName()))
                .doOnError(error -> log.error("Error obteniendo tipos de préstamo activos: {}", error.getMessage()));
    }
    
    @Override
    public Flux<LoanType> findByAutomaticValidationEnabled() {
        log.debug("Obteniendo tipos de préstamo con validación automática habilitada");
        
        String sql = """
            SELECT * FROM loan_types 
            WHERE automatic_validation = true AND is_active = true 
            ORDER BY type_code
            """;
        
        return template.getDatabaseClient()
                .sql(sql)
                .fetch()
                .all()
                .map(this::mapRowToEntity)
                .map(this::mapToDomain)
                .doOnNext(loanType -> log.debug("Tipo con validación automática: {}", loanType.getDisplayName()))
                .doOnError(error -> log.error("Error obteniendo tipos con validación automática: {}", error.getMessage()));
    }
    
    @Override
    public Mono<Boolean> requiresAutomaticValidation(LoanType.LoanTypeCode typeCode) {
        log.debug("Verificando si el tipo {} requiere validación automática", typeCode);
        
        String sql = """
            SELECT automatic_validation FROM loan_types 
            WHERE type_code = :type_code AND is_active = true
            """;
            
        return template.getDatabaseClient()
                .sql(sql)
                .bind("type_code", typeCode.getCode())
                .fetch()
                .first()
                .map(row -> {
                    Object value = row.get("automatic_validation");
                    // Manejar tanto Boolean como Byte (MySQL puede devolver 0/1 como Byte)
                    if (value instanceof Boolean) {
                        return (Boolean) value;
                    } else if (value instanceof Byte) {
                        return ((Byte) value) == 1;
                    } else if (value instanceof Number) {
                        return ((Number) value).intValue() == 1;
                    } else {
                        log.warn("Tipo inesperado para automatic_validation: {} ({})", value, value != null ? value.getClass() : "null");
                        return false;
                    }
                })
                .defaultIfEmpty(false)
                .doOnNext(requires -> log.debug("Tipo {} requiere validación automática: {}", typeCode, requires))
                .doOnError(error -> log.error("Error verificando validación automática para {}: {}", typeCode, error.getMessage()));
    }
    
    /**
     * Convierte una fila de la BD a LoanTypeEntity
     */
    private LoanTypeEntity mapRowToEntity(Map<String, Object> row) {
        return LoanTypeEntity.builder()
                .id((Integer) row.get("id"))
                .typeCode((String) row.get("type_code"))
                .displayName((String) row.get("display_name"))
                .description((String) row.get("description"))
                .automaticValidation(convertToBoolean(row.get("automatic_validation")))
                .defaultInterestRate((java.math.BigDecimal) row.get("default_interest_rate"))
                .minAmount((java.math.BigDecimal) row.get("min_amount"))
                .maxAmount((java.math.BigDecimal) row.get("max_amount"))
                .minTermMonths((Integer) row.get("min_term_months"))
                .maxTermMonths((Integer) row.get("max_term_months"))
                .isActive(convertToBoolean(row.get("is_active")))
                .createdAt((java.time.LocalDateTime) row.get("created_at"))
                .updatedAt((java.time.LocalDateTime) row.get("updated_at"))
                .build();
    }
    
    /**
     * Convierte valores de MySQL (que pueden ser Byte, Boolean, etc.) a Boolean
     */
    private Boolean convertToBoolean(Object value) {
        if (value == null) {
            return false;
        } else if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof Byte) {
            return ((Byte) value) == 1;
        } else if (value instanceof Number) {
            return ((Number) value).intValue() == 1;
        } else {
            log.warn("Tipo inesperado para campo boolean: {} ({})", value, value.getClass());
            return false;
        }
    }
    
    /**
     * Convierte LoanTypeEntity a dominio LoanType
     */
    private LoanType mapToDomain(LoanTypeEntity entity) {
        return LoanType.builder()
                .id(entity.getId())
                .typeCode(LoanType.LoanTypeCode.fromCode(entity.getTypeCode()))
                .displayName(entity.getDisplayName())
                .description(entity.getDescription())
                .automaticValidation(entity.getAutomaticValidation())
                .defaultInterestRate(entity.getDefaultInterestRate())
                .minAmount(entity.getMinAmount())
                .maxAmount(entity.getMaxAmount())
                .minTermMonths(entity.getMinTermMonths())
                .maxTermMonths(entity.getMaxTermMonths())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
