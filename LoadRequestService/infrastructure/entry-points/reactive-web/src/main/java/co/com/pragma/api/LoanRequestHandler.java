package co.com.pragma.api;

import co.com.pragma.api.dto.ApiResponse;
import co.com.pragma.api.dto.PagedResponse;
import co.com.pragma.api.security.SecurityContextHelper;
import co.com.pragma.model.common.MessageFormatter;
import co.com.pragma.model.common.Messages;
import co.com.pragma.model.loan.LoanDecisionDTO;
import co.com.pragma.model.loan.LoanRequest;
import co.com.pragma.model.loan.LoanRequestDTO;
import co.com.pragma.model.loan.LoanRequestReviewDTO;
import co.com.pragma.usecase.loan.LoanRequestUseCase;
import co.com.pragma.usecase.loan.ListLoanRequestsForReviewUseCase;
import co.com.pragma.usecase.loan.ProcessLoanDecisionUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoanRequestHandler {
    
    private final LoanRequestUseCase loanRequestUseCase;
    private final ListLoanRequestsForReviewUseCase listLoanRequestsForReviewUseCase;
    private final ProcessLoanDecisionUseCase processLoanDecisionUseCase;

    /**
     * Crea una nueva solicitud de préstamo
     * Solo accesible para usuarios con rol CLIENTE
     * Valida que el cliente solo pueda crear solicitudes para sí mismo
     */
    public Mono<ServerResponse> createLoanRequest(ServerRequest serverRequest) {
        // Extraer información del usuario autenticado
        SecurityContextHelper.AuthenticatedUser authenticatedUser = 
                SecurityContextHelper.getAuthenticatedUser(serverRequest);
        
        log.info(MessageFormatter.format(Messages.LOG_USER_ACTION, 
                 authenticatedUser.getEmail(), "iniciando", "creación de solicitud de préstamo"));

        return serverRequest.bodyToMono(LoanRequestDTO.class)
                .flatMap(loanRequestDTO -> {
                    // Validar que el cliente solo pueda crear solicitudes para sí mismo
                    if (!SecurityContextHelper.validateUserAccess(loanRequestDTO.getClientDocumentId(), authenticatedUser)) {
                        log.warn(MessageFormatter.format(Messages.LOG_USER_ACTION, 
                               authenticatedUser.getEmail(), "intentó crear solicitud para cliente diferente:", loanRequestDTO.getClientDocumentId()));
                        return ServerResponse.status(HttpStatus.FORBIDDEN)
                                .bodyValue(ApiResponse.error(Messages.AUTH_ACCESS_DENIED));
                    }
                    
                    // Si la validación pasa, crear la solicitud y devolver la respuesta
                    return loanRequestUseCase.createLoanRequest(loanRequestDTO)
                            .flatMap(createdLoanRequest -> {
                                log.info(MessageFormatter.format(Messages.LOG_USER_ACTION, 
                                       authenticatedUser.getEmail(), "creó solicitud exitosamente - ID:", createdLoanRequest.getId()));
                                
                                ApiResponse<LoanRequest> response = ApiResponse.success(
                                        createdLoanRequest,
                                        Messages.LOAN_REQUEST_CREATED
                                );
                                return ServerResponse.status(HttpStatus.CREATED).bodyValue(response);
                            });
                })
                .onErrorResume(IllegalArgumentException.class, this::handleValidationError)
                .onErrorResume(Exception.class, this::handleGenericError);
    }

    /**
     * Actualiza una solicitud de préstamo existente
     * Solo accesible para usuarios con rol ASESOR
     */
    public Mono<ServerResponse> updateLoanRequest(ServerRequest serverRequest) {
        SecurityContextHelper.AuthenticatedUser authenticatedUser = 
                SecurityContextHelper.getAuthenticatedUser(serverRequest);
        
        String loanRequestId = serverRequest.pathVariable("id");
        
        log.info(MessageFormatter.format(Messages.LOG_USER_ACTION, 
                authenticatedUser.getEmail(), "iniciando actualización de solicitud:", loanRequestId));

        return serverRequest.bodyToMono(LoanRequestDTO.class)
                .flatMap(loanRequestDTO -> loanRequestUseCase.updateLoanRequest(loanRequestId, loanRequestDTO))
                .flatMap(updatedLoanRequest -> {
                    log.info(MessageFormatter.format(Messages.LOG_USER_ACTION, 
                           authenticatedUser.getEmail(), "actualizó solicitud exitosamente - ID:", loanRequestId));
                    
                    ApiResponse<LoanRequest> response = ApiResponse.success(
                            updatedLoanRequest, 
                            Messages.LOAN_REQUEST_UPDATED
                    );
                    return ServerResponse.ok().bodyValue(response);
                })
                .onErrorResume(IllegalArgumentException.class, this::handleValidationError)
                .onErrorResume(Exception.class, this::handleGenericError);
    }

    /**
     * Lista todas las solicitudes de préstamo
     * Solo accesible para usuarios con rol ASESOR
     */
    public Mono<ServerResponse> getAllLoanRequests(ServerRequest serverRequest) {
        SecurityContextHelper.AuthenticatedUser authenticatedUser = 
                SecurityContextHelper.getAuthenticatedUser(serverRequest);
        
        log.info(MessageFormatter.format(Messages.LOG_USER_ACTION, 
                authenticatedUser.getEmail(), "consultando", "todas las solicitudes de préstamo"));

        return loanRequestUseCase.getAllLoanRequests()
                .collectList()
                .flatMap(loanRequests -> {
                    log.info(MessageFormatter.format("Se retornaron {0} solicitudes al asesor: {1}", 
                           loanRequests.size(), authenticatedUser.getEmail()));
                    
                    ApiResponse<java.util.List<LoanRequest>> response = ApiResponse.success(
                            loanRequests,
                            MessageFormatter.format(Messages.OPERATION_SUCCESS_RETRIEVED, "solicitudes")
                    );
                    return ServerResponse.ok().bodyValue(response);
                })
                .onErrorResume(Exception.class, this::handleGenericError);
    }

    /**
     * Lista las solicitudes que requieren revisión manual
     * Solo accesible para usuarios con rol ASESOR
     * Retorna un listado paginado y filtrable de solicitudes pendientes de decisión
     */
    public Mono<ServerResponse> getSolicitudesForReview(ServerRequest serverRequest) {
        SecurityContextHelper.AuthenticatedUser authenticatedUser = 
                SecurityContextHelper.getAuthenticatedUser(serverRequest);
        
        log.info(MessageFormatter.format(Messages.LOG_USER_ACTION, 
                authenticatedUser.getEmail(), "consultando", "solicitudes para revisión manual"));

        // Extraer parámetros de paginación
        int page = serverRequest.queryParam("page")
                .map(Integer::parseInt)
                .orElse(0);
        
        int size = serverRequest.queryParam("size")
                .map(Integer::parseInt)
                .orElse(10);

        log.info("DEBUG - Parámetros recibidos: page={}, size={}, asesor={}", 
                page, size, authenticatedUser.getEmail());

        // Obtener datos paginados y conteo total
        Mono<PagedResponse<LoanRequestReviewDTO>> pagedResponseMono = 
                listLoanRequestsForReviewUseCase.listSolicitudesForReview(page, size, authenticatedUser.getEmail())
                        .collectList()
                        .zipWith(listLoanRequestsForReviewUseCase.countSolicitudesForReview(authenticatedUser.getEmail()))
                        .map(tuple -> {
                            var solicitudes = tuple.getT1();
                            var totalElements = tuple.getT2();
                            
                            log.info("DEBUG - Resultados obtenidos: solicitudes en página={}, total en BD={}, page={}, size={}", 
                                   solicitudes.size(), totalElements, page, size);
                            
                            return PagedResponse.of(solicitudes, page, size, totalElements);
                        });

        return pagedResponseMono
                .flatMap(pagedResponse -> {
                    String message = Messages.LOAN_REQUESTS_RETRIEVED;
                    
                    // Agregar información adicional si la página está vacía pero hay elementos
                    if (pagedResponse.isEmpty() && pagedResponse.getTotalElements() > 0) {
                        message += MessageFormatter.format(Messages.PAGINATION_EMPTY_PAGE_INFO, 
                                page, pagedResponse.getTotalElements(), pagedResponse.getTotalPages());
                    }
                    
                    ApiResponse<PagedResponse<LoanRequestReviewDTO>> response = ApiResponse.success(
                            pagedResponse, 
                            message
                    );
                    return ServerResponse.ok().bodyValue(response);
                })
                .onErrorResume(IllegalArgumentException.class, this::handleValidationError)
                .onErrorResume(Exception.class, this::handleGenericError);
    }

    private Mono<ServerResponse> handleValidationError(IllegalArgumentException error) {
        ApiResponse<String> response = ApiResponse.error(error.getMessage());
        
        return ServerResponse.status(HttpStatus.BAD_REQUEST)
                .bodyValue(response);
    }

    private Mono<ServerResponse> handleGenericError(Exception error) {
        log.error("Error inesperado en LoanRequestHandler", error);
        
        ApiResponse<String> response = ApiResponse.error(Messages.ERROR_UNEXPECTED);
        
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .bodyValue(response);
    }

    /**
     * Procesa decisiones de aprobación/rechazo de solicitudes
     * Solo accesible para usuarios con rol ASESOR
     */
    public Mono<ServerResponse> processLoanDecision(ServerRequest request) {
        log.info(MessageFormatter.format(Messages.LOG_OPERATION_STARTED, "procesamiento de decisión", "solicitud de préstamo"));

        // Extraer información completa del usuario autenticado
        SecurityContextHelper.AuthenticatedUser authenticatedUser = 
                SecurityContextHelper.getAuthenticatedUser(request);
        
        String asesorEmail = authenticatedUser.getEmail();
        String asesorId = authenticatedUser.getId();
        
        if (asesorEmail == null || asesorId == null) {
            return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                    .bodyValue(ApiResponse.error(Messages.AUTH_USER_NOT_AUTHENTICATED));
        }

        log.info(MessageFormatter.format(Messages.LOG_USER_ACTION, asesorEmail, "procesando", "decisión de solicitud"));
        
        return request.bodyToMono(LoanDecisionDTO.class)
                .doOnNext(decision -> {
                    log.debug("DEBUG - Decisión recibida: solicitudId={}, decision={}, asesor={}, asesorId={}", 
                             decision.getSolicitudId(), decision.getDecision(), asesorEmail, asesorId);
                    
                    // Establecer tanto el email como el ID del asesor desde el token JWT
                    decision.setAsesorEmail(asesorEmail);
                })
                .flatMap(decision -> processLoanDecisionUseCase.processDecision(decision, asesorId))
                .flatMap(updatedRequest -> {
                    ApiResponse<LoanRequest> response = ApiResponse.success(
                            updatedRequest,
                            MessageFormatter.format(Messages.LOAN_DECISION_PROCESSED, 
                                         updatedRequest.getId(), asesorEmail)
                    );
                    return ServerResponse.ok().bodyValue(response);
                })
                .onErrorResume(IllegalArgumentException.class, this::handleValidationError)
                .onErrorResume(IllegalStateException.class, error -> {
                    ApiResponse<String> response = ApiResponse.error(
                            "Estado de solicitud inválido: " + error.getMessage()
                    );
                    return ServerResponse.status(HttpStatus.CONFLICT).bodyValue(response);
                })
                .onErrorResume(Exception.class, this::handleGenericError);
    }
}
