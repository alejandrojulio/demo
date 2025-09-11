package co.com.pragma.model.common;

import java.text.MessageFormat;

/**
 * Utilidad para formatear mensajes con parámetros
 * Proporciona métodos para reemplazar placeholders en mensajes de forma segura
 */
public final class MessageFormatter {

    private MessageFormatter() {
        // Constructor privado para evitar instanciación
    }

    /**
     * Formatea un mensaje reemplazando placeholders {0}, {1}, etc. con los parámetros proporcionados
     * 
     * @param pattern El patrón del mensaje con placeholders
     * @param params Los parámetros a reemplazar
     * @return El mensaje formateado
     */
    public static String format(String pattern, Object... params) {
        if (pattern == null) {
            return "";
        }
        
        if (params == null || params.length == 0) {
            return pattern;
        }
        
        try {
            return MessageFormat.format(pattern, params);
        } catch (Exception e) {
            // En caso de error, devolver el patrón original con información de debug
            return pattern + " [Error formatting with params: " + java.util.Arrays.toString(params) + "]";
        }
    }

    /**
     * Formatea un mensaje con un solo parámetro
     * 
     * @param pattern El patrón del mensaje
     * @param param El parámetro a reemplazar
     * @return El mensaje formateado
     */
    public static String format(String pattern, Object param) {
        return format(pattern, new Object[]{param});
    }

    /**
     * Formatea un mensaje con dos parámetros
     * 
     * @param pattern El patrón del mensaje
     * @param param1 El primer parámetro
     * @param param2 El segundo parámetro
     * @return El mensaje formateado
     */
    public static String format(String pattern, Object param1, Object param2) {
        return format(pattern, new Object[]{param1, param2});
    }

    /**
     * Formatea un mensaje con tres parámetros
     * 
     * @param pattern El patrón del mensaje
     * @param param1 El primer parámetro
     * @param param2 El segundo parámetro
     * @param param3 El tercer parámetro
     * @return El mensaje formateado
     */
    public static String format(String pattern, Object param1, Object param2, Object param3) {
        return format(pattern, new Object[]{param1, param2, param3});
    }
}
