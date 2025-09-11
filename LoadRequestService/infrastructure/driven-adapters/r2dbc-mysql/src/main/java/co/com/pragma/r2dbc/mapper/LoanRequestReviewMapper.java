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
     * Inmutable, sin side effects, composable
     */
    public static final Function<LoanRequestReviewProjection, LoanRequestReviewDTO> toDTO = 
        projection -> LoanRequestReviewDTO.builder()
            .id(projection.getId())
            .monto(projection.getAmount())
            .plazo(projection.getTermInMonths())
            .email(projection.getEmail())
            .nombre(buildFullName(projection.getFirstName(), projection.getLastName()))
            .tipoPrestamo(projection.getLoanType())
            .tasaInteres(projection.getInterestRate())
            .estadoSolicitud(projection.getStatus())
            .fechaCreacion(projection.getCreatedAt())
            .fechaActualizacion(projection.getUpdatedAt())
            .salarioBase(projection.getBaseSalary())
            .deudaTotalMensualSolicitudesAprobadas(projection.getMonthlyDebt())
            .documentoCliente(projection.getClientDocument())
            .notas(projection.getNotes())
            .build();

    /**
     * Función pura para construir nombre completo
     * Maneja casos null/empty de forma funcional
     */
    private static String buildFullName(String firstName, String lastName) {
        return String.join(" ", 
            nullToEmpty(firstName), 
            nullToEmpty(lastName)
        ).trim();
    }

    /**
     * Función auxiliar pura para manejo de nulls
     */
    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
