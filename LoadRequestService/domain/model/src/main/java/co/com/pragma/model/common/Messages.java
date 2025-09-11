package co.com.pragma.model.common;

/**
 * Constantes centralizadas para mensajes del sistema CrediYa
 * Organizadas por categorías para facilitar el mantenimiento
 */
public final class Messages {

    private Messages() {
        // Constructor privado para evitar instanciación
    }

    // =========================================
    // MENSAJES DE VALIDACIÓN GENÉRICOS
    // =========================================
    
    public static final String VALIDATION_REQUIRED_FIELD = "El campo {0} es obligatorio";
    public static final String VALIDATION_INVALID_RANGE = "El campo {0} debe estar entre {1} y {2}";
    public static final String VALIDATION_MUST_BE_POSITIVE = "El campo {0} debe ser mayor a cero";
    public static final String VALIDATION_INVALID_FORMAT = "El formato del campo {0} no es válido";
    public static final String VALIDATION_ALREADY_EXISTS = "El {0} ya existe: {1}";
    public static final String VALIDATION_NOT_FOUND = "No se encontró {0} con {1}: {2}";
    
    // =========================================
    // MENSAJES DE AUTENTICACIÓN Y AUTORIZACIÓN
    // =========================================
    
    public static final String AUTH_USER_NOT_AUTHENTICATED = "No se encontró información del usuario autenticado";
    public static final String AUTH_INVALID_CREDENTIALS = "Credenciales inválidas";
    public static final String AUTH_ACCESS_DENIED = "No tiene permisos para acceder a este recurso";
    public static final String AUTH_INVALID_TOKEN = "Token inválido o expirado";
    public static final String AUTH_INSUFFICIENT_PERMISSIONS = "Usuario {0} sin permisos para: {1} {2} (rol: {3})";
    
    // =========================================
    // MENSAJES DE OPERACIONES CRUD
    // =========================================
    
    public static final String OPERATION_SUCCESS_CREATED = "{0} creado exitosamente";
    public static final String OPERATION_SUCCESS_UPDATED = "{0} actualizado exitosamente";
    public static final String OPERATION_SUCCESS_RETRIEVED = "{0} obtenido exitosamente";
    public static final String OPERATION_SUCCESS_PROCESSED = "{0} procesado exitosamente";
    
    // =========================================
    // MENSAJES DE ERROR GENÉRICOS
    // =========================================
    
    public static final String ERROR_UNEXPECTED = "Ha ocurrido un error inesperado. Por favor, inténtelo más tarde";
    public static final String ERROR_INVALID_STATE = "Estado inválido: {0}";
    public static final String ERROR_OPERATION_NOT_ALLOWED = "Operación no permitida en el estado actual";
    public static final String ERROR_INVALID_PARAMETERS = "Parámetros inválidos: {0}";
    
    // =========================================
    // MENSAJES DE LOGGING GENÉRICOS
    // =========================================
    
    public static final String LOG_OPERATION_STARTED = "Iniciando {0} para: {1}";
    public static final String LOG_OPERATION_COMPLETED = "{0} completado exitosamente para: {1}";
    public static final String LOG_OPERATION_FAILED = "Error en {0} para: {1} - Error: {2}";
    public static final String LOG_VALIDATION_FAILED = "Validación fallida en {0}: {1}";
    public static final String LOG_USER_ACTION = "Usuario {0} {1} {2}";
    
    // =========================================
    // MENSAJES ESPECÍFICOS DE SOLICITUDES
    // =========================================
    
    public static final String LOAN_REQUEST_CREATED = "Solicitud de préstamo creada exitosamente";
    public static final String LOAN_REQUEST_UPDATED = "Solicitud de préstamo actualizada exitosamente";
    public static final String LOAN_REQUESTS_RETRIEVED = "Solicitudes para revisión obtenidas exitosamente";
    public static final String LOAN_DECISION_PROCESSED = "Decisión procesada exitosamente - Solicitud {0} por asesor {1}";
    
    public static final String LOAN_VALIDATION_AMOUNT_RANGE = "El monto del préstamo debe ser mayor a $100,000 y menor a $50,000,000";
    public static final String LOAN_VALIDATION_TERM_RANGE = "El plazo del préstamo debe estar entre 1 y 120 meses";
    public static final String LOAN_VALIDATION_EXISTING = "El cliente ya tiene una solicitud de préstamo {0} pendiente o activa";
    public static final String LOAN_VALIDATION_STATE_APPROVED = "La solicitud ya está aprobada";
    public static final String LOAN_VALIDATION_STATE_REJECTED = "La solicitud ya está rechazada";
    public static final String LOAN_VALIDATION_STATE_CANCELLED = "La solicitud está cancelada";
    
    public static final String LOAN_DECISION_VALIDATION_REQUIRED = "La decisión (APPROVED/REJECTED) es requerida";
    public static final String LOAN_DECISION_VALIDATION_INVALID = "La decisión debe ser APPROVED o REJECTED";
    public static final String LOAN_APPROVAL_AMOUNT_REQUIRED = "El monto aprobado es requerido y debe ser mayor a cero";
    public static final String LOAN_APPROVAL_RATE_REQUIRED = "La tasa de interés es requerida y debe ser mayor a cero";
    public static final String LOAN_APPROVAL_TERM_REQUIRED = "El plazo aprobado es requerido y debe ser mayor a cero";
    public static final String LOAN_REJECTION_REASON_REQUIRED = "El motivo de rechazo es requerido";
    
    // =========================================
    // MENSAJES DE NOTIFICACIONES
    // =========================================
    
    public static final String NOTIFICATION_SENT_SUCCESS = "Notificación enviada exitosamente para solicitud {0}";
    public static final String NOTIFICATION_SEND_FAILED = "Error al enviar notificación para solicitud {0}: {1}";
    public static final String NOTIFICATION_MESSAGE_CREATED = "Mensaje de notificación creado para solicitud {0}, tipo: {1}";
    
    // =========================================
    // MENSAJES DE PAGINACIÓN
    // =========================================
    
    public static final String PAGINATION_EMPTY_PAGE_INFO = " (Página {0} está vacía. Hay {1} elementos totales en {2} páginas. Las páginas empiezan desde 0)";
    public static final String PAGINATION_INVALID_PAGE = "El número de página debe ser mayor o igual a 0";
    public static final String PAGINATION_INVALID_SIZE = "El tamaño de página debe estar entre 1 y 100";
    
    // =========================================
    // VALORES DE CONFIGURACIÓN
    // =========================================
    
    public static final class Config {
        public static final String MIN_LOAN_AMOUNT = "100000";
        public static final String MAX_LOAN_AMOUNT = "50000000";
        public static final String MIN_LOAN_TERM = "1";
        public static final String MAX_LOAN_TERM = "120";
        public static final String MIN_PAGE_SIZE = "1";
        public static final String MAX_PAGE_SIZE = "100";
        
        private Config() {}
    }
    
    // =========================================
    // HEADERS Y CONSTANTES TÉCNICAS
    // =========================================
    
    public static final class Headers {
        public static final String USER_ID = "X-User-Id";
        public static final String USER_EMAIL = "X-User-Email";
        public static final String USER_ROLE = "X-User-Role";
        public static final String USER_DOCUMENT = "X-User-Document";
        public static final String TOKEN = "token";
        public static final String AUTHORIZATION = "Authorization";
        public static final String BEARER_PREFIX = "Bearer ";
        
        private Headers() {}
    }
    
    // =========================================
    // ENDPOINTS
    // =========================================
    
    public static final class Endpoints {
        public static final String LOAN_REQUEST_BASE = "/api/v1/solicitud";
        public static final String LOAN_REQUEST_BY_ID = "/api/v1/solicitud/{id}";
        public static final String AUTH_LOGIN = "/api/v1/login";
        public static final String AUTH_VALIDATE_TOKEN = "/api/v1/auth/validate-token";
        public static final String USERS_BASE = "/api/v1/users";
        public static final String USERS_BY_ID = "/api/v1/users/{id}";
        
        private Endpoints() {}
    }
}
