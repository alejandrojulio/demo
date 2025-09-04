package co.com.pragma.usecase.loan;

/**
 * Constantes para mensajes y textos utilizados en LoanRequestUseCase
 */
public final class LoanRequestUseCaseConstants {

    private LoanRequestUseCaseConstants() {
        // Constructor privado para evitar instanciación
    }

    // Mensajes de logging
    public static final String LOG_CREATING_LOAN_REQUEST = "Iniciando proceso de creación de solicitud de préstamo para cliente: {}";
    public static final String LOG_VALIDATING_LOAN_DATA = "Validando datos de la solicitud de préstamo para cliente: {}";
    public static final String LOG_LOAN_REQUEST_CREATED_SUCCESS = "Solicitud de préstamo creada exitosamente con ID: {}";
    public static final String LOG_ERROR_CREATING_LOAN_REQUEST = "Error en la creación de solicitud de préstamo para cliente: {}";
    
    public static final String LOG_VALIDATING_CLIENT_EXISTS = "Validando existencia del cliente con documento: {}";
    public static final String LOG_CLIENT_VALIDATION_SUCCESS = "Cliente validado correctamente: {}";
    public static final String LOG_CLIENT_NOT_FOUND = "Cliente no encontrado con documento: {}";
    
    public static final String LOG_VALIDATING_LOAN_AMOUNT = "Validando monto del préstamo: {} para cliente: {}";
    public static final String LOG_LOAN_AMOUNT_VALID = "Monto del préstamo válido para cliente: {}";
    
    public static final String LOG_VALIDATING_LOAN_TERM = "Validando plazo del préstamo: {} meses para cliente: {}";
    public static final String LOG_LOAN_TERM_VALID = "Plazo del préstamo válido para cliente: {}";
    
    public static final String LOG_VALIDATING_LOAN_TYPE = "Validando tipo de préstamo: {} para cliente: {}";
    public static final String LOG_LOAN_TYPE_VALID = "Tipo de préstamo válido para cliente: {}";
    
    public static final String LOG_CHECKING_EXISTING_LOAN = "Verificando préstamos existentes para cliente: {} y tipo: {}";
    public static final String LOG_EXISTING_LOAN_FOUND = "Préstamo existente encontrado para cliente: {} y tipo: {}";
    public static final String LOG_NO_EXISTING_LOAN = "No se encontraron préstamos existentes para cliente: {} y tipo: {}";
    
    public static final String LOG_ENRICHING_LOAN_DATA = "Enriqueciendo datos de la solicitud para cliente: {}";
    public static final String LOG_SAVING_LOAN_REQUEST = "Guardando solicitud de préstamo para cliente: {}";

    // Mensajes de error de validación
    public static final String ERROR_CLIENT_DOCUMENT_REQUIRED = "El documento de identidad del cliente es obligatorio";
    public static final String ERROR_CLIENT_NOT_FOUND = "El cliente con documento {} no fue encontrado";
    public static final String ERROR_LOAN_AMOUNT_REQUIRED = "El monto del préstamo es obligatorio";
    public static final String ERROR_LOAN_AMOUNT_INVALID = "El monto del préstamo debe ser mayor a $100,000 y menor a $50,000,000";
    public static final String ERROR_LOAN_TERM_REQUIRED = "El plazo del préstamo es obligatorio";
    public static final String ERROR_LOAN_TERM_INVALID = "El plazo del préstamo debe estar entre 1 y 120 meses";
    public static final String ERROR_LOAN_TYPE_REQUIRED = "El tipo de préstamo es obligatorio";
    public static final String ERROR_LOAN_TYPE_INVALID = "El tipo de préstamo {} no es válido";
    public static final String ERROR_EXISTING_LOAN = "El cliente ya tiene una solicitud de préstamo {} pendiente o activa";
    public static final String ERROR_INVALID_LOAN_DATA = "Los datos de la solicitud de préstamo no son válidos";

    // Valores de configuración
    public static final String MIN_LOAN_AMOUNT = "100000";
    public static final String MAX_LOAN_AMOUNT = "50000000";
    public static final String MIN_LOAN_TERM = "1";
    public static final String MAX_LOAN_TERM = "120";
}
