package co.com.pragma.model;

import co.com.pragma.model.loan.LoanDecisionDTO;
import co.com.pragma.model.loan.LoanRequest;
import co.com.pragma.model.loan.LoanRequestDTO;
import co.com.pragma.model.loan.LoanRequestReviewDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Builder de datos de prueba para LoadRequestService
 * Proporciona métodos para crear objetos de test con datos consistentes
 */
public final class TestDataBuilder {

    private TestDataBuilder() {
        // Utility class
    }

    // ==========================================
    // LOAN REQUEST BUILDERS
    // ==========================================

    public static LoanRequest.LoanRequestBuilder defaultLoanRequest() {
        return LoanRequest.builder()
                .id(1L)
                .clientDocumentId("12345678")
                .amount(new BigDecimal("10000000"))
                .termInMonths(36)
                .loanType(LoanRequest.LoanType.PERSONAL)
                .status(LoanRequest.LoanStatus.PENDING_REVIEW)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .notes("Solicitud de préstamo personal para consolidación de deudas")
                .approvedAmount(null)
                .interestRate(null)
                .monthlyPayment(null)
                .approvedAt(null)
                .approvedBy(null)
                .rejectionReason(null);
    }

    public static LoanRequest.LoanRequestBuilder vehicleLoanRequest() {
        return defaultLoanRequest()
                .id(2L)
                .amount(new BigDecimal("25000000"))
                .termInMonths(60)
                .loanType(LoanRequest.LoanType.VEHICLE)
                .notes("Préstamo para compra de vehículo nuevo");
    }

    public static LoanRequest.LoanRequestBuilder homeLoanRequest() {
        return defaultLoanRequest()
                .id(3L)
                .amount(new BigDecimal("80000000"))
                .termInMonths(120)
                .loanType(LoanRequest.LoanType.HOME)
                .notes("Préstamo hipotecario para vivienda");
    }

    public static LoanRequest.LoanRequestBuilder businessLoanRequest() {
        return defaultLoanRequest()
                .id(4L)
                .amount(new BigDecimal("15000000"))
                .termInMonths(48)
                .loanType(LoanRequest.LoanType.BUSINESS)
                .notes("Préstamo para capital de trabajo");
    }

    public static LoanRequest.LoanRequestBuilder approvedLoanRequest() {
        return defaultLoanRequest()
                .status(LoanRequest.LoanStatus.APPROVED)
                .approvedAmount(new BigDecimal("10000000"))
                .interestRate(new BigDecimal("15.5"))
                .monthlyPayment(new BigDecimal("350000"))
                .approvedAt(LocalDateTime.now())
                .approvedBy("asesor@crediya.com");
    }

    public static LoanRequest.LoanRequestBuilder rejectedLoanRequest() {
        return defaultLoanRequest()
                .status(LoanRequest.LoanStatus.REJECTED)
                .rejectionReason("Ingresos insuficientes para el monto solicitado");
    }

    public static LoanRequest.LoanRequestBuilder manualReviewLoanRequest() {
        return defaultLoanRequest()
                .status(LoanRequest.LoanStatus.MANUAL_REVIEW)
                .amount(new BigDecimal("45000000"))
                .notes("Solicitud requiere revisión manual por monto alto");
    }

    public static LoanRequest.LoanRequestBuilder cancelledLoanRequest() {
        return defaultLoanRequest()
                .status(LoanRequest.LoanStatus.CANCELLED)
                .notes("Solicitud cancelada por el cliente");
    }

    // ==========================================
    // LOAN REQUEST DTO BUILDERS
    // ==========================================

    public static LoanRequestDTO.LoanRequestDTOBuilder defaultLoanRequestDTO() {
        return LoanRequestDTO.builder()
                .clientDocumentId("12345678")
                .amount(new BigDecimal("8000000"))
                .termInMonths(24)
                .loanType(LoanRequest.LoanType.PERSONAL)
                .notes("Nueva solicitud de préstamo personal");
    }

    public static LoanRequestDTO.LoanRequestDTOBuilder invalidLoanRequestDTO() {
        return defaultLoanRequestDTO()
                .amount(new BigDecimal("50000"))  // Monto muy bajo
                .termInMonths(0);  // Plazo inválido
    }

    public static LoanRequestDTO.LoanRequestDTOBuilder maxAmountLoanRequestDTO() {
        return defaultLoanRequestDTO()
                .amount(new BigDecimal("60000000"))  // Monto muy alto
                .termInMonths(150);  // Plazo muy largo
    }

    // ==========================================
    // LOAN DECISION BUILDERS
    // ==========================================

    public static LoanDecisionDTO.LoanDecisionDTOBuilder approvalDecision() {
        return LoanDecisionDTO.builder()
                .solicitudId(1L)
                .decision(LoanRequest.LoanStatus.APPROVED)
                .asesorEmail("asesor@crediya.com")
                .montoAprobado(new BigDecimal("10000000"))
                .tasaInteres(new BigDecimal("15.5"))
                .plazoAprobado(36);
    }

    public static LoanDecisionDTO.LoanDecisionDTOBuilder rejectionDecision() {
        return LoanDecisionDTO.builder()
                .solicitudId(1L)
                .decision(LoanRequest.LoanStatus.REJECTED)
                .asesorEmail("asesor@crediya.com")
                .motivo("Ingresos insuficientes para el monto solicitado");
    }

    public static LoanDecisionDTO.LoanDecisionDTOBuilder invalidApprovalDecision() {
        return LoanDecisionDTO.builder()
                .solicitudId(1L)
                .decision(LoanRequest.LoanStatus.APPROVED)
                .asesorEmail("asesor@crediya.com")
                // Faltan campos requeridos para aprobación
                .montoAprobado(null)
                .tasaInteres(null)
                .plazoAprobado(null);
    }

    public static LoanDecisionDTO.LoanDecisionDTOBuilder invalidRejectionDecision() {
        return LoanDecisionDTO.builder()
                .solicitudId(1L)
                .decision(LoanRequest.LoanStatus.REJECTED)
                .asesorEmail("asesor@crediya.com")
                // Falta motivo de rechazo
                .motivo(null);
    }

    // ==========================================
    // LOAN REQUEST REVIEW DTO BUILDERS
    // ==========================================

    public static LoanRequestReviewDTO.LoanRequestReviewDTOBuilder defaultReviewDTO() {
        return LoanRequestReviewDTO.builder()
                .id(1L)
                .monto(new BigDecimal("10000000"))
                .plazo(36)
                .email("cliente@crediya.com")
                .nombre("Juan Pérez")
                .tipoPrestamo("PERSONAL")
                .tasaInteres(new BigDecimal("15.5"))
                .estadoSolicitud("PENDING_REVIEW")
                .fechaCreacion(LocalDateTime.now())
                .salarioBase(new BigDecimal("3000000"))
                .deudaTotalMensualSolicitudesAprobadas(new BigDecimal("500000"))
                .documentoCliente("12345678")
                .notas("Solicitud pendiente de revisión");
    }

    // ==========================================
    // SECURITY CONTEXT BUILDERS
    // ==========================================

    public static class SecurityContextBuilder {
        public static final String ADMIN_EMAIL = "admin@crediya.com";
<<<<<<< HEAD
        public static final String ADVISOR_EMAIL = "asesor@crediya.com";
        public static final String CLIENT_EMAIL = "cliente@crediya.com";
        
        public static final String ADMIN_ROLE = "ADMIN";
        public static final String ADVISOR_ROLE = "ADVISOR";
        public static final String CLIENT_ROLE = "CLIENT";
        
        public static final String ADMIN_DOCUMENT = "admin001";
        public static final String ADVISOR_DOCUMENT = "asesor001";
        public static final String CLIENT_DOCUMENT = "12345678";

        // Constantes legacy para compatibilidad
        @Deprecated
        public static final String ASESOR_EMAIL = ADVISOR_EMAIL;
        @Deprecated
        public static final String CLIENTE_EMAIL = CLIENT_EMAIL;
        @Deprecated
        public static final String ASESOR_ROLE = "ASESOR";
        @Deprecated
        public static final String CLIENTE_ROLE = "CLIENTE";
        @Deprecated
        public static final String ASESOR_DOCUMENT = ADVISOR_DOCUMENT;
        @Deprecated
        public static final String CLIENTE_DOCUMENT = CLIENT_DOCUMENT;
=======
        public static final String ASESOR_EMAIL = "asesor@crediya.com";
        public static final String CLIENTE_EMAIL = "cliente@crediya.com";
        
        public static final String ADMIN_ROLE = "ADMINISTRADOR";
        public static final String ASESOR_ROLE = "ASESOR";
        public static final String CLIENTE_ROLE = "CLIENTE";
        
        public static final String ADMIN_DOCUMENT = "admin001";
        public static final String ASESOR_DOCUMENT = "asesor001";
        public static final String CLIENTE_DOCUMENT = "12345678";
>>>>>>> origin/main
    }

    // ==========================================
    // CONSTANT VALUES
    // ==========================================

    public static final class TestConstants {
        // Montos
        public static final BigDecimal MIN_LOAN_AMOUNT = new BigDecimal("100000");
        public static final BigDecimal MAX_LOAN_AMOUNT = new BigDecimal("50000000");
        public static final BigDecimal VALID_LOAN_AMOUNT = new BigDecimal("10000000");
        public static final BigDecimal INVALID_LOW_AMOUNT = new BigDecimal("50000");
        public static final BigDecimal INVALID_HIGH_AMOUNT = new BigDecimal("60000000");
        
        // Plazos
        public static final Integer MIN_LOAN_TERM = 1;
        public static final Integer MAX_LOAN_TERM = 120;
        public static final Integer VALID_LOAN_TERM = 36;
        public static final Integer INVALID_LOW_TERM = 0;
        public static final Integer INVALID_HIGH_TERM = 150;
        
        // Tasas de interés por tipo de préstamo
        public static final BigDecimal PERSONAL_INTEREST_RATE = new BigDecimal("15.5");
        public static final BigDecimal VEHICLE_INTEREST_RATE = new BigDecimal("12.8");
        public static final BigDecimal HOME_INTEREST_RATE = new BigDecimal("9.2");
        public static final BigDecimal BUSINESS_INTEREST_RATE = new BigDecimal("18.3");
        
        // Documentos de prueba
        public static final String VALID_CLIENT_DOCUMENT = "12345678";
        public static final String ANOTHER_CLIENT_DOCUMENT = "87654321";
        public static final String NONEXISTENT_CLIENT_DOCUMENT = "99999999";
        
        // IDs de prueba
        public static final Long VALID_LOAN_ID = 1L;
        public static final Long NONEXISTENT_LOAN_ID = 999L;
        
        // Mensajes
        public static final String LOAN_CREATION_SUCCESS = "Solicitud de préstamo creada exitosamente";
        public static final String LOAN_UPDATE_SUCCESS = "Solicitud de préstamo actualizada exitosamente";
        public static final String LOAN_RETRIEVAL_SUCCESS = "Solicitudes para revisión obtenidas exitosamente";
        public static final String DECISION_PROCESSED_SUCCESS = "Decisión procesada exitosamente";
        
        private TestConstants() {}
    }
}
