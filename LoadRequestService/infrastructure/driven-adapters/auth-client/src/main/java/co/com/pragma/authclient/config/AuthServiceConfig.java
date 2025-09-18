package co.com.pragma.authclient.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuración para cliente HTTP hacia AuthService
 */
@Configuration
@Slf4j
public class AuthServiceConfig {

    @Value("${app.auth-service.base-url:http://auth-service:8080}")
    private String authServiceBaseUrl;

    @Value("${app.auth-service.timeout-seconds:10}")
    private int timeoutSeconds;

    @Value("${app.auth-service.connection-pool-size:10}")
    private int connectionPoolSize;

    @Bean
    public WebClient authServiceWebClient() {
        log.info("Configurando WebClient para AuthService en: {}", authServiceBaseUrl);

        // Configurar HttpClient con timeouts y pool de conexiones
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutSeconds * 1000)
                .responseTimeout(Duration.ofSeconds(timeoutSeconds))
                .doOnConnected(conn -> {
                    conn.addHandlerLast(new ReadTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS));
                    conn.addHandlerLast(new WriteTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS));
                });

        return WebClient.builder()
                .baseUrl(authServiceBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest())
                .filter(logResponse())
                .filter(handleErrors())
                .build();
    }

    /**
     * Filter para logging de requests
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if (log.isDebugEnabled()) {
                log.debug("Request: {} {}", clientRequest.method(), clientRequest.url());
            }
            return Mono.just(clientRequest);
        });
    }

    /**
     * Filter para logging de responses
     */
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (log.isDebugEnabled()) {
                log.debug("Response: {} {}", clientResponse.statusCode(), clientResponse.headers());
            }
            return Mono.just(clientResponse);
        });
    }

    /**
     * Filter para manejo de errores HTTP
     */
    private ExchangeFilterFunction handleErrors() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (clientResponse.statusCode().is4xxClientError()) {
                log.warn("Error de cliente en AuthService: {}", clientResponse.statusCode());
            } else if (clientResponse.statusCode().is5xxServerError()) {
                log.error("Error de servidor en AuthService: {}", clientResponse.statusCode());
            }
            return Mono.just(clientResponse);
        });
    }
}
