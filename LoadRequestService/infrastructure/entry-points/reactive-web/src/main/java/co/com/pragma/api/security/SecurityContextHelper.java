package co.com.pragma.api.security;

import co.com.pragma.model.common.Messages;
import lombok.Builder;
import lombok.Data;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

/**
 * Helper para extraer información de seguridad de los headers de la request
 */
public class SecurityContextHelper {

    private static final String USER_ID_HEADER = Messages.Headers.USER_ID;
    private static final String USER_EMAIL_HEADER = Messages.Headers.USER_EMAIL;
    private static final String USER_ROLE_HEADER = Messages.Headers.USER_ROLE;
    private static final String USER_DOCUMENT_HEADER = Messages.Headers.USER_DOCUMENT;

    /**
     * Extrae la información del usuario autenticado de los headers de la request
     */
    public static AuthenticatedUser getAuthenticatedUser(ServerRequest request) {
        String userId = request.headers().firstHeader(USER_ID_HEADER);
        String userEmail = request.headers().firstHeader(USER_EMAIL_HEADER);
        String userRole = request.headers().firstHeader(USER_ROLE_HEADER);
        String userDocument = request.headers().firstHeader(USER_DOCUMENT_HEADER);
        
        if (userId == null || userEmail == null || userRole == null || userDocument == null) {
            // Log de debug para ayudar con el troubleshooting
            System.out.println("DEBUG - Headers encontrados:");
            System.out.println("X-User-Id: " + userId);
            System.out.println("X-User-Email: " + userEmail);
            System.out.println("X-User-Role: " + userRole);
            System.out.println("X-User-Document: " + userDocument);
            System.out.println("Todos los headers: " + request.headers().asHttpHeaders().toSingleValueMap());
        }
        
        return AuthenticatedUser.builder()
                .id(userId)
                .email(userEmail)
                .role(userRole)
                .document(userDocument)
                .build();
    }

    /**
     * Extrae el email del usuario autenticado de los headers
     */
    public static Mono<String> extractUserEmail(ServerRequest request) {
        String userEmail = request.headers().firstHeader(USER_EMAIL_HEADER);
        
        if (userEmail == null || userEmail.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException(Messages.AUTH_USER_NOT_AUTHENTICATED));
        }
        
        return Mono.just(userEmail);
    }

    /**
     * Valida que el documento del usuario autenticado coincida con el documento solicitado
     * Útil para validar que un cliente solo acceda a sus propios recursos
     */
    public static boolean validateUserAccess(String requestedDocumentId, AuthenticatedUser authenticatedUser) {
        return authenticatedUser.getDocument() != null && 
               authenticatedUser.getDocument().equals(requestedDocumentId);
    }

    /**
     * Información del usuario autenticado extraída del token JWT
     */
    @Data
    @Builder
    public static class AuthenticatedUser {
        private String id;
        private String email;
        private String role;
        private String document;

        public boolean hasRole(String role) {
            return this.role != null && this.role.equalsIgnoreCase(role);
        }

        public boolean isCliente() {
            return hasRole("CLIENTE");
        }

        public boolean isAsesor() {
            return hasRole("ASESOR");
        }

        public boolean isAdministrador() {
            return hasRole("ADMINISTRADOR");
        }
    }
}
