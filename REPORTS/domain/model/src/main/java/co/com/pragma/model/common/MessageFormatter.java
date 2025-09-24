package co.com.pragma.model.common;

import java.text.MessageFormat;

public class MessageFormatter {
    
    public static String format(String pattern, Object... arguments) {
        if (pattern == null) {
            return "";
        }
        
        if (arguments == null || arguments.length == 0) {
            return pattern;
        }
        
        try {
            return MessageFormat.format(pattern, arguments);
        } catch (Exception e) {
            // Si hay error en el formateo, retornar el patrón original
            return pattern + " [Error en formateo: " + String.join(", ", 
                    java.util.Arrays.stream(arguments)
                            .map(String::valueOf)
                            .toArray(String[]::new)) + "]";
        }
    }
}
