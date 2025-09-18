package co.com.pragma.r2dbc.mapper;

import co.com.pragma.model.loan.LoanRequestReviewDTO;
import co.com.pragma.r2dbc.projection.LoanRequestReviewProjection;

import java.util.function.Function;

/**
 * Mapper funcional para transformaciones puras
 * Siguiendo principios de programación funcional
 */
public final class LoanRequestReviewMapper {

    private LoanRequestReviewMapper() {
        // Utility class - no instantiation
    }

    /**
     * Función pura para mapear projection a DTO
     * NOTA: Los datos de usuario (email, nombre, salario) ahora se obtienen vía AuthService HTTP API
     * Este mapper crea DTOs con valores temporales que serán enriquecidos en el use case
     */
    public static final Function<LoanRequestReviewProjection, LoanRequestReviewDTO> toDTO = 
        projection -> LoanRequestReviewDTO.builder()
            .id(projection.getId())
            .monto(projection.getAmount())
            .plazo(projection.getTermInMonths())
            .email("") // Se obtendrá vía AuthService
            .nombre("") // Se obtendrá vía AuthService
            .tipoPrestamo(projection.getLoanType())
            .tasaInteres(projection.getInterestRate())
            .estadoSolicitud(projection.getStatus())
            .fechaCreacion(projection.getCreatedAt())
            .fechaActualizacion(projection.getUpdatedAt())
            .salarioBase(java.math.BigDecimal.ZERO) // Se obtendrá vía AuthService
            .deudaTotalMensualSolicitudesAprobadas(projection.getMonthlyDebt())
            .documentoCliente(projection.getClientDocument())
            .notas(projection.getNotes())
            .build();

    // Funciones auxiliares eliminadas - Los datos de usuario ahora se obtienen vía AuthService
}
