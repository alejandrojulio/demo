package co.com.pragma.events.config;

import co.com.pragma.events.LogEventPublisher;
import co.com.pragma.usecase.report.gateways.EventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventPublisherConfig {
    
    @Bean
    public EventPublisher eventPublisher() {
        return new LogEventPublisher();
    }
}
