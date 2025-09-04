package co.com.pragma.usecase.user;


public final class UserUseCaseConstants {

    private UserUseCaseConstants() {
    }

    public static final String LOG_SEARCHING_USER = "Buscando usuario con ID: {}";
    public static final String LOG_USER_FOUND = "Usuario encontrado con ID: {}";
    public static final String LOG_USER_NOT_FOUND = "Usuario no encontrado con ID: {}";
    public static final String LOG_ERROR_SEARCHING_USER = "Error al buscar usuario con ID: {}";
    
    public static final String LOG_CREATING_USER = "Iniciando proceso de creación de usuario para email: {}";
    public static final String LOG_VALIDATING_USER_DATA = "Validando datos del usuario: {}";
    public static final String LOG_USER_CREATED_SUCCESS = "Usuario creado exitosamente con ID: {}";
    public static final String LOG_ERROR_CREATING_USER = "Error en la creación de usuario para email: {}";
    
    public static final String LOG_VALIDATING_REQUIRED_FIELDS = "Validando campos obligatorios para usuario: {}";
    public static final String LOG_REQUIRED_FIELDS_VALIDATED = "Campos obligatorios validados correctamente para: {}";
    
    public static final String LOG_VALIDATING_EMAIL_FORMAT = "Validando formato de email para: {}";
    public static final String LOG_EMAIL_FORMAT_VALID = "Formato de email válido para: {}";
    
    public static final String LOG_VALIDATING_SALARY_RANGE = "Validando rango de salario para usuario: {} - Salario: {}";
    public static final String LOG_SALARY_RANGE_VALID = "Rango de salario válido para usuario: {}";
    
    public static final String LOG_CHECKING_EMAIL_UNIQUENESS = "Verificando unicidad de email: {}";
    public static final String LOG_EMAIL_ALREADY_EXISTS = "Intento de registro con email ya existente: {}";
    public static final String LOG_EMAIL_UNIQUE_CONFIRMED = "Email único confirmado para: {}";
    public static final String LOG_ERROR_CHECKING_EMAIL_UNIQUENESS = "Error al verificar unicidad del email: {}";
    
    public static final String LOG_ENRICHING_USER_DATA = "Enriqueciendo datos del usuario: {}";

    public static final String ERROR_FIRST_NAME_REQUIRED = "El nombre es obligatorio";
    public static final String ERROR_LAST_NAME_REQUIRED = "El apellido es obligatorio";
    public static final String ERROR_EMAIL_REQUIRED = "El correo electrónico es obligatorio";
    public static final String ERROR_BASE_SALARY_REQUIRED = "El salario base es obligatorio";
    public static final String ERROR_INVALID_EMAIL_FORMAT = "El formato del correo electrónico no es válido";
    public static final String ERROR_SALARY_BELOW_MIN = "El salario base no puede ser menor a 0";
    public static final String ERROR_SALARY_ABOVE_MAX = "El salario base no puede ser mayor a 15,000,000";
    public static final String ERROR_EMAIL_ALREADY_REGISTERED = "El correo electrónico ya está registrado: {}";

    public static final String MAX_SALARY_DISPLAY = "15,000,000";
}
