package co.com.pragma.helper;

import co.com.pragma.model.user.gateways.ApplicationLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ApplicationLoggerImpl implements ApplicationLogger {
    
    private static final Logger logger = LoggerFactory.getLogger(ApplicationLoggerImpl.class);

    @Override
    public void info(String message, Object... args) {
        logger.info(message, args);
    }

    @Override
    public void warn(String message, Object... args) {
        logger.warn(message, args);
    }

    @Override
    public void error(String message, Throwable t) {
        logger.error(message, t);
    }
}
