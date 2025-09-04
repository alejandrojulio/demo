package co.com.pragma.model.loan.gateways;

public interface LoanApplicationLogger {
    void info(String message, Object... args);
    void warn(String message, Object... args);
    void error(String message, Throwable throwable);
    void error(String message);
}
