package co.com.pragma.model.common;

import java.text.MessageFormat;

public final class MessageFormatter {

    private MessageFormatter() {
    }

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
            return pattern + " [Error formatting with params: " + java.util.Arrays.toString(params) + "]";
        }
    }

    public static String format(String pattern, Object param) {
        return format(pattern, new Object[]{param});
    }

    public static String format(String pattern, Object param1, Object param2) {
        return format(pattern, new Object[]{param1, param2});
    }

    public static String format(String pattern, Object param1, Object param2, Object param3) {
        return format(pattern, new Object[]{param1, param2, param3});
    }
}
