package co.com.pragma.helpers;

import co.com.pragma.model.loan.gateways.LoanApplicationLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LoanApplicationLoggerAdapter implements LoanApplicationLogger {

    @Override
    public void info(String message, Object... args) {
        log.info(message, args);
    }

    @Override
    public void warn(String message, Object... args) {
        log.warn(message, args);
    }

    @Override
    public void error(String message, Throwable throwable) {
        log.error(message, throwable);
    }

    @Override
    public void error(String message) {
        log.error(message);
    }
}
