package co.com.pragma.api;

import co.com.pragma.api.auth.AuthHandler;
import co.com.pragma.api.config.ServiceAuthenticationFilter;
import co.com.pragma.model.common.Messages;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {
    
    @Bean
    public RouterFunction<ServerResponse> routerFunction(Handler handler, AuthHandler authHandler, 
                                                        UserDataHandler userDataHandler, ServiceAuthenticationFilter serviceFilter) {
        
        // Rutas públicas (sin autenticación)
        RouterFunction<ServerResponse> publicRoutes = route(GET(Messages.Endpoints.USERS_BY_ID), handler::listenGETUseCase)
                .andRoute(POST(Messages.Endpoints.USERS_BASE), handler::listenPOSTUseCase)
                .andRoute(POST(Messages.Endpoints.AUTH_LOGIN), authHandler::login)
                .andRoute(POST(Messages.Endpoints.AUTH_VALIDATE_TOKEN), authHandler::validateToken);
        
        // Rutas internas para microservicios (CON autenticación de servicio)
        RouterFunction<ServerResponse> internalRoutes = route(GET("/api/v1/users/document/{document}"), userDataHandler::getUserByDocument)
                .andRoute(GET("/api/v1/users/email/{email}"), userDataHandler::getUserByEmail)
                .andRoute(GET("/api/v1/users/validate/{document}"), userDataHandler::validateUserExists)
                .filter(serviceFilter); // Aplicar filtro solo a rutas internas
        
        return publicRoutes.and(internalRoutes);
    }
}
