package co.com.pragma.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class JwtServerAuthenticationConverter implements ServerAuthenticationConverter {

    private static final String TOKEN_HEADER = "token";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        return Mono.fromCallable(() -> {
            // Primero intentar obtener token del header "token"
            String token = exchange.getRequest()
                    .getHeaders()
                    .getFirst(TOKEN_HEADER);

            if (token != null && !token.trim().isEmpty()) {
                log.debug("Token extraído del header 'token'");
                return new UsernamePasswordAuthenticationToken(token, token);
            }

            // Si no existe, intentar con Authorization Bearer (por compatibilidad)
            String authorization = exchange.getRequest()
                    .getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

            if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
                String bearerToken = authorization.substring(BEARER_PREFIX.length());
                log.debug("Token extraído del header Authorization Bearer");
                return new UsernamePasswordAuthenticationToken(bearerToken, bearerToken);
            }

            log.debug("No se encontró token en headers 'token' ni 'Authorization'");
            return null;
        });
    }
}
