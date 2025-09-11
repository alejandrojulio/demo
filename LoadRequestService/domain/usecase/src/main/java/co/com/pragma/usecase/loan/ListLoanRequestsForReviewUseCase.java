package co.com.pragma.usecase.loan;

import co.com.pragma.model.loan.LoanRequest;
import co.com.pragma.model.loan.LoanRequestReviewDTO;
import co.com.pragma.model.loan.gateways.LoanApplicationLogger;
import co.com.pragma.model.loan.gateways.LoanRequestRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Caso de uso para listar solicitudes que requieren revisión manual
 * Solo accesible para usuarios con rol ASESOR
 */
public class ListLoanRequestsForReviewUseCase {

    private final LoanRequestRepository loanRequestRepository;
    private final LoanApplicationLogger logger;

    // Estados que requieren revisión manual - Inmutable
    private static final List<String> ESTADOS_REVISION_MANUAL = List.of(
            LoanRequest.LoanStatus.PENDING_REVIEW.name(),
            LoanRequest.LoanStatus.REJECTED.name(),
            LoanRequest.LoanStatus.MANUAL_REVIEW.name()
    );

    // Predicados funcionales para validación
    private static final Predicate<Integer> isValidPage = page -> page >= 0;
    private static final Predicate<Integer> isValidSize = size -> size > 0 && size <= 100;

    // Función para crear mensajes de error
    private static final Function<String, IllegalArgumentException> createValidationError = 
        message -> new IllegalArgumentException(message);

    public ListLoanRequestsForReviewUseCase(LoanRequestRepository loanRequestRepository,
                                          LoanApplicationLogger logger) {
        this.loanRequestRepository = loanRequestRepository;
        this.logger = logger;
    }

    /**
     * Lista las solicitudes que requieren revisión manual con paginación
     * 
     * @param page Número de página (base 0)
     * @param size Tamaño de página
     * @param userEmail Email del asesor que realiza la consulta
     * @return Flux de solicitudes para revisión
     */
    public Flux<LoanRequestReviewDTO> listSolicitudesForReview(int page, int size, String userEmail) {
        return validateParameters(page, size, userEmail)
                .flatMapMany(params -> executeQuery(params.page, params.size, userEmail))
                .doOnNext(solicitud -> logSolicitudFound(solicitud, userEmail))
                .doOnComplete(() -> logQueryCompleted(userEmail))
                .doOnError(error -> logQueryError(userEmail, error));
    }

    /**
     * Validación funcional de parámetros
     * Retorna Mono con parámetros validados o error
     */
    private Mono<QueryParams> validateParameters(int page, int size, String userEmail) {
        logger.info("Iniciando consulta de solicitudes para revisión manual - Asesor: {}, Página: {}, Tamaño: {}", 
                   userEmail, page, size);

        return Mono.just(new QueryParams(page, size))
                .filter(params -> isValidPage.test(params.page))
                .switchIfEmpty(Mono.defer(() -> {
                    logger.warn("Parámetro de página inválido - Página: {}, Asesor: {}", page, userEmail);
                    return Mono.error(createValidationError.apply("El número de página debe ser mayor o igual a 0"));
                }))
                .filter(params -> isValidSize.test(params.size))
                .switchIfEmpty(Mono.defer(() -> {
                    logger.warn("Parámetro de tamaño de página inválido - Tamaño: {}, Asesor: {}", size, userEmail);
                    return Mono.error(createValidationError.apply("El tamaño de página debe estar entre 1 y 100"));
                }));
    }

    /**
     * Ejecución funcional de la consulta
     */
    private Flux<LoanRequestReviewDTO> executeQuery(int page, int size, String userEmail) {
        return loanRequestRepository.findSolicitudesForManualReview(page, size, ESTADOS_REVISION_MANUAL);
    }

    /**
     * Funciones puras para logging
     */
    private void logSolicitudFound(LoanRequestReviewDTO solicitud, String userEmail) {
        logger.info("Solicitud encontrada para revisión - ID: {}, Estado: {}", 
                   solicitud.getId(), solicitud.getEstadoSolicitud());
    }

    private void logQueryCompleted(String userEmail) {
        logger.info("Consulta de solicitudes para revisión completada - Asesor: {}", userEmail);
    }

    private void logQueryError(String userEmail, Throwable error) {
        logger.error("Error al consultar solicitudes para revisión - Asesor: " + userEmail, error);
    }

    /**
     * Record inmutable para parámetros de consulta
     */
    private record QueryParams(int page, int size) {}

    /**
     * Cuenta el total de solicitudes que requieren revisión manual
     * 
     * @param userEmail Email del asesor que realiza la consulta
     * @return Mono con el conteo total
     */
    public Mono<Long> countSolicitudesForReview(String userEmail) {
        logger.info("Contando solicitudes para revisión manual - Asesor: {}", userEmail);

        return loanRequestRepository.countSolicitudesForManualReview(ESTADOS_REVISION_MANUAL)
                .doOnNext(count -> logger.info("Total de solicitudes para revisión encontradas - Cantidad: {}, Asesor: {}", 
                         count, userEmail))
                .doOnError(error -> {
                    logger.error("Error al contar solicitudes para revisión - Asesor: " + userEmail, error);
                });
    }
}
