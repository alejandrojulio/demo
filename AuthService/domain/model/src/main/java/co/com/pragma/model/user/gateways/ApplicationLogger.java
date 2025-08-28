package co.com.pragma.model.user.gateways;

public interface ApplicationLogger {
    void info(String message, Object... args);
    void warn(String message, Object... args);
    void error(String message, Throwable t);
}