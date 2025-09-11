package co.com.pragma.model.common;

/**
 * Constantes centralizadas para mensajes del sistema CrediYa - AuthService
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
    // MENSAJES DE AUTENTICACIÓN
    // =========================================
    
    public static final String AUTH_INVALID_CREDENTIALS = "Invalid credentials";
    public static final String AUTH_INTERNAL_ERROR = "Internal server error";
    public static final String AUTH_INVALID_TOKEN = "Invalid or expired token";
    public static final String AUTH_ACCESS_DENIED = "Access denied";
    public static final String AUTH_VALID_TOKEN = "Valid token";
    public static final String AUTH_SUCCESS = "Authentication successful";
    public static final String AUTH_VALIDATION_ERROR = "Validation error";
    
    // =========================================
    // MENSAJES DE USUARIO
    // =========================================
    
    public static final String USER_PASSWORD_REQUIRED = "Password is required";
    public static final String USER_PASSWORD_MIN_LENGTH = "Password must be at least 6 characters long";
    public static final String USER_EMAIL_INVALID_FORMAT = "El formato del correo electrónico no es válido";
    public static final String USER_EMAIL_ALREADY_REGISTERED = "El correo electrónico ya está registrado: {0}";
    public static final String USER_SALARY_BELOW_MIN = "El salario base no puede ser menor a 0";
    public static final String USER_SALARY_ABOVE_MAX = "El salario base no puede ser mayor a 15,000,000";
    
    // =========================================
    // MENSAJES DE LOGGING
    // =========================================
    
    public static final String LOG_AUTH_STARTED = "Iniciando proceso de autenticación para usuario: {0}";
    public static final String LOG_AUTH_FAILED_USER_NOT_FOUND = "Intento de login fallido - Usuario no encontrado: {0}";
    public static final String LOG_AUTH_FAILED_WRONG_PASSWORD = "Intento de login fallido - Contraseña incorrecta para usuario: {0}";
    public static final String LOG_AUTH_SUCCESS = "Credenciales válidas para usuario: {0}";
    public static final String LOG_AUTH_ERROR = "Error durante el proceso de autenticación para usuario: {0} - Error: {1}";
    
    public static final String LOG_TOKEN_VALIDATION_STARTED = "Validando token JWT";
    public static final String LOG_TOKEN_INVALID = "Token JWT inválido o expirado";
    public static final String LOG_TOKEN_VALID = "Token válido para usuario: {0} con rol: {1}";
    public static final String LOG_TOKEN_GENERATED = "Token JWT generado exitosamente para usuario: {0} con rol: {1}";
    public static final String LOG_TOKEN_ERROR = "Error inesperado validando token: {0}";
    public static final String LOG_TOKEN_EXTRACT_ERROR = "Error extrayendo {0} del token: {1}";
    
    public static final String LOG_PASSWORD_ENCRYPTION = "Encriptando contraseña para usuario: {0}";
    public static final String LOG_PASSWORD_VALIDATION = "Validando requisitos de contraseña para usuario: {0}";
    public static final String LOG_PASSWORD_VALID = "Contraseña válida para usuario: {0}";
    
    // =========================================
    // MENSAJES DE ERROR GENÉRICOS
    // =========================================
    
    public static final String ERROR_TOKEN_INVALID = "Token inválido";
    public static final String ERROR_JWT_INVALID = "Token JWT inválido: {0}";
    
    // =========================================
    // VALORES DE CONFIGURACIÓN
    // =========================================
    
    public static final class Config {
        public static final String MAX_SALARY_DISPLAY = "15,000,000";
        public static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$";
        public static final String DEFAULT_JWT_SECRET = "myDefaultSecretKeyForCrediYaApplicationThatIsLongEnough";
        public static final int DEFAULT_JWT_EXPIRATION_HOURS = 24;
        public static final int MIN_PASSWORD_LENGTH = 6;
        
        private Config() {}
    }
    
    // =========================================
    // HEADERS Y CONSTANTES TÉCNICAS
    // =========================================
    
    public static final class Headers {
        public static final String TOKEN = "token";
        public static final String AUTHORIZATION = "Authorization";
        public static final String BEARER_PREFIX = "Bearer ";
        
        private Headers() {}
    }
    
    // =========================================
    // ENDPOINTS
    // =========================================
    
    public static final class Endpoints {
        public static final String AUTH_LOGIN = "/api/v1/login";
        public static final String AUTH_VALIDATE_TOKEN = "/api/v1/auth/validate-token";
        public static final String USERS_BASE = "/api/v1/users";
        public static final String USERS_BY_ID = "/api/v1/users/{id}";
        public static final String ACTUATOR_HEALTH = "/actuator/**";
        public static final String HEALTH = "/health";
        
        private Endpoints() {}
    }
    
    // =========================================
    // CLAIMS JWT
    // =========================================
    
    public static final class JwtClaims {
        public static final String EMAIL = "email";
        public static final String ROLE = "role";
        public static final String DOCUMENT = "document";
        
        private JwtClaims() {}
    }
}
