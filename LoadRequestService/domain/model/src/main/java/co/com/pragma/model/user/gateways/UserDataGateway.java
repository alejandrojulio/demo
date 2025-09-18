package co.com.pragma.model.user.gateways;

import co.com.pragma.model.user.UserData;
import reactor.core.publisher.Mono;

/**
 * Puerto para obtener datos de usuarios desde AuthService
 * Implementa el patrón de comunicación entre microservicios
 */
public interface UserDataGateway {
    
    /**
     * Obtiene los datos de un usuario por su documento de identidad
     * @param document documento de identidad del usuario
     * @return datos del usuario o error si no existe
     */
    Mono<UserData> getUserByDocument(String document);
    
    /**
     * Obtiene los datos de un usuario por su email
     * @param email email del usuario
     * @return datos del usuario o error si no existe
     */
    Mono<UserData> getUserByEmail(String email);
    
    /**
     * Valida si un usuario existe y está activo
     * @param document documento de identidad del usuario
     * @return true si el usuario existe y está activo, false en caso contrario
     */
    Mono<Boolean> validateUserExists(String document);
}
