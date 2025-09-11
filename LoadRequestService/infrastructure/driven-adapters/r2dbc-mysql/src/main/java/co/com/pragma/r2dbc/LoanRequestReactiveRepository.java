package co.com.pragma.r2dbc;

import co.com.pragma.r2dbc.entity.LoanRequestEntity;
import co.com.pragma.r2dbc.projection.LoanRequestReviewProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Repository
public interface LoanRequestReactiveRepository extends ReactiveCrudRepository<LoanRequestEntity, Long>, ReactiveQueryByExampleExecutor<LoanRequestEntity> {
    
    Mono<Boolean> existsByClientDocumentIdAndLoanType(String clientDocumentId, String loanType);
    
    /**
     * Consulta funcional usando Query Methods
     * Spring Data R2DBC maneja automáticamente el IN con múltiples parámetros
     */
    @Query("""
        SELECT 
            lr.id as id,
            lr.amount as amount,
            lr.term_in_months as termInMonths,
            lr.loan_type as loanType,
            lr.status as status,
            lr.created_at as createdAt,
            lr.updated_at as updatedAt,
            lr.client_document as clientDocument,
            COALESCE(lr.notes, '') as notes,
            COALESCE(u.email, '') as email,
            COALESCE(u.first_name, '') as firstName,
            COALESCE(u.last_name, '') as lastName,
            COALESCE(u.base_salary, 0) as baseSalary,
            CASE 
                WHEN lr.loan_type = 'PERSONAL' THEN 15.5
                WHEN lr.loan_type = 'VEHICLE' THEN 12.8
                WHEN lr.loan_type = 'HOME' THEN 9.2
                WHEN lr.loan_type = 'BUSINESS' THEN 18.3
                ELSE 15.0
            END as interestRate,
            COALESCE(
                (SELECT SUM(deuda.amount * 0.02) 
                 FROM loan_requests deuda 
                 WHERE deuda.client_document = lr.client_document 
                 AND deuda.status = 'APPROVED'), 0
            ) as monthlyDebt
        FROM loan_requests lr
        LEFT JOIN users u ON lr.client_document = u.document
        WHERE lr.status IN ('PENDING_REVIEW', 'REJECTED', 'MANUAL_REVIEW')
        ORDER BY lr.created_at DESC
        LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}
        """)
    Flux<LoanRequestReviewProjection> findSolicitudesForManualReview(Pageable pageable);
    
    /**
     * Conteo funcional para paginación
     */
    @Query("""
        SELECT COUNT(*) 
        FROM loan_requests lr
        LEFT JOIN users u ON lr.client_document = u.document
        WHERE lr.status IN ('PENDING_REVIEW', 'REJECTED', 'MANUAL_REVIEW')
        """)
    Mono<Long> countSolicitudesForManualReview();
}
