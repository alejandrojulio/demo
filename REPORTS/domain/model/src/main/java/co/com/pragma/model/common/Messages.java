package co.com.pragma.model.common;

public class Messages {
    
    // Mensajes de éxito
    public static final String REPORT_RETRIEVED = "Reporte de préstamos obtenido exitosamente";
    public static final String REPORT_UPDATED = "Contador de préstamos actualizado exitosamente";
    
    // Mensajes de error
    public static final String ERROR_UNEXPECTED = "Ha ocurrido un error inesperado. Por favor, intente nuevamente.";
    public static final String ERROR_REPORT_NOT_FOUND = "No se encontró el reporte de préstamos";
    public static final String ERROR_INVALID_EVENT = "Evento de préstamo inválido";
    public static final String ERROR_DYNAMODB_CONNECTION = "Error de conexión con la base de datos";
    public static final String ERROR_SQS_PROCESSING = "Error al procesar el mensaje SQS";
    
    // Logs de trazabilidad
    public static final String LOG_OPERATION_STARTED = "Operación iniciada: {0} para {1}";
    public static final String LOG_OPERATION_COMPLETED = "Operación completada: {0} para {1}";
    public static final String LOG_EVENT_RECEIVED = "Evento recibido: {0} - Préstamo ID: {1}";
    public static final String LOG_COUNTER_UPDATED = "Contador actualizado - Total: {0}, Último préstamo: {1}";
    public static final String LOG_REPORT_RETRIEVED = "Reporte consultado - Total préstamos: {0}";
    
    // Endpoints
    public static class Endpoints {
        public static final String REPORTS_BASE = "/api/v1/reportes";
        public static final String LOAN_COUNTER = "/api/v1/reportes/prestamos-aprobados";
    }
}
