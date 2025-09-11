package co.com.pragma.model.loan.gateways;

import co.com.pragma.model.loan.LoanRequest;
import co.com.pragma.model.loan.LoanRequestReviewDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface LoanRequestRepository {
    Mono<LoanRequest> save(LoanRequest loanRequest);
    Mono<LoanRequest> findById(Long id);
    Mono<LoanRequest> update(LoanRequest loanRequest);
    Flux<LoanRequest> findAll();
    Mono<Boolean> existsByClientDocumentAndType(String clientDocumentId, LoanRequest.LoanType loanType);
    
    /**
     * Obtiene las solicitudes que requieren revisión manual con información del cliente
     * Incluye solicitudes con estado: PENDING_REVIEW, REJECTED, MANUAL_REVIEW
     */
    Flux<LoanRequestReviewDTO> findSolicitudesForManualReview(int page, int size, List<String> estados);
    
    /**
     * Cuenta las solicitudes que requieren revisión manual
     */
    Mono<Long> countSolicitudesForManualReview(List<String> estados);
    
    /**
     * Obtiene una solicitud con información completa del cliente para notificaciones
     */
    Mono<LoanRequestReviewDTO> findByIdWithClientInfo(Long id);
}
