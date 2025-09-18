package co.com.pragma.r2dbc;

import co.com.pragma.model.loan.LoanRequest;
import co.com.pragma.model.loan.LoanRequestReviewDTO;
import co.com.pragma.model.loan.gateways.LoanRequestRepository;
import co.com.pragma.r2dbc.entity.LoanRequestEntity;
import co.com.pragma.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class LoanRequestRepositoryAdapter extends ReactiveAdapterOperations<
        LoanRequest,
        LoanRequestEntity,
        Long,
        LoanRequestReactiveRepository>
        implements LoanRequestRepository {

    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    public LoanRequestRepositoryAdapter(LoanRequestReactiveRepository repository, 
                                       ObjectMapper mapper,
                                       R2dbcEntityTemplate r2dbcEntityTemplate) {
        super(repository, mapper, d -> mapper.map(d, LoanRequest.class));
        this.r2dbcEntityTemplate = r2dbcEntityTemplate;
    }

    @Override
    public Mono<LoanRequest> save(LoanRequest loanRequest) {
        return super.save(loanRequest);
    }

    @Override
    public Mono<LoanRequest> findById(Long id) {
        return super.findById(id);
    }

    @Override
    public Mono<LoanRequest> update(LoanRequest loanRequest) {
        return super.save(loanRequest);
    }

    @Override
    public reactor.core.publisher.Flux<LoanRequest> findAll() {
        return super.findAll();
    }

    @Override
    public Mono<Boolean> existsByClientDocumentAndTypeAndPendingStatus(String clientDocumentId, LoanRequest.LoanType loanType) {
        return repository.existsByClientDocumentIdAndLoanTypeAndStatus(
                clientDocumentId, 
                loanType.name(), 
                LoanRequest.LoanStatus.PENDING_REVIEW.name()
        );
    }

    @Override
    public Flux<LoanRequestReviewDTO> findSolicitudesForManualReview(int page, int size, List<String> estados) {
        String sql = """
            SELECT 
                lr.id,
                lr.amount,
                lr.term_in_months,
                lr.loan_type,
                lr.status,
                lr.created_at,
                lr.updated_at,
                lr.client_document,
                COALESCE(lr.notes, '') as notes
            FROM loan_requests lr
            WHERE lr.status IN ('PENDING_REVIEW', 'REJECTED', 'MANUAL_REVIEW')
            ORDER BY lr.created_at DESC
            LIMIT :size OFFSET :offset
            """;

        int offset = page * size;
        
        return r2dbcEntityTemplate
                .getDatabaseClient()
                .sql(sql)
                .bind("size", size)
                .bind("offset", offset)
                .map(this::mapRowToDTO)
                .all();
    }

    @Override
    public Mono<Long> countSolicitudesForManualReview(List<String> estados) {
        return repository.countSolicitudesForManualReview();
    }
    
    @Override
    public Mono<LoanRequestReviewDTO> findByIdWithClientInfo(Long id) {
        String sql = """
            SELECT 
                lr.id,
                lr.amount,
                lr.term_in_months,
                lr.loan_type,
                lr.status,
                lr.created_at,
                lr.updated_at,
                lr.client_document,
                COALESCE(lr.notes, '') as notes,
                COALESCE(lr.approved_amount, 0) as approvedAmount,
                COALESCE(lr.interest_rate, 0) as interestRate,
                COALESCE(lr.monthly_payment, 0) as monthlyPayment,
                COALESCE(lr.rejection_reason, '') as rejectionReason
            FROM loan_requests lr
            WHERE lr.id = :id
            """;
            
        return r2dbcEntityTemplate
                .getDatabaseClient()
                .sql(sql)
                .bind("id", id)
                .map(this::mapRowToCompleteDTO)
                .one();
    }


    private LoanRequestReviewDTO mapRowToDTO(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        return LoanRequestReviewDTO.builder()
                .id(row.get("id", Long.class))
                .monto(row.get("amount", BigDecimal.class))
                .plazo(row.get("term_in_months", Integer.class))
                .email("") // Se obtendrá vía AuthService
                .nombre("") // Se obtendrá vía AuthService
                .tipoPrestamo(row.get("loan_type", String.class))
                .tasaInteres(calculateInterestRate(row.get("loan_type", String.class)))
                .estadoSolicitud(row.get("status", String.class))
                .fechaCreacion(row.get("created_at", LocalDateTime.class))
                .fechaActualizacion(row.get("updated_at", LocalDateTime.class))
                .salarioBase(BigDecimal.ZERO) // Se obtendrá vía AuthService
                .deudaTotalMensualSolicitudesAprobadas(BigDecimal.ZERO) // Calculado por separado
                .documentoCliente(row.get("client_document", String.class))
                .notas(row.get("notes", String.class))
                .build();
    }

    private String buildFullName(String firstName, String lastName) {
        if (firstName == null && lastName == null) return "";
        if (firstName == null) return lastName;
        if (lastName == null) return firstName;
        return (firstName + " " + lastName).trim();
    }

    private LoanRequestReviewDTO mapRowToCompleteDTO(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        return LoanRequestReviewDTO.builder()
                .id(row.get("id", Long.class))
                .monto(row.get("amount", BigDecimal.class))
                .plazo(row.get("term_in_months", Integer.class))
                .email("") // Se obtendrá vía AuthService
                .nombre("") // Se obtendrá vía AuthService
                .tipoPrestamo(row.get("loan_type", String.class))
                .tasaInteres(row.get("interestRate", BigDecimal.class))
                .estadoSolicitud(row.get("status", String.class))
                .fechaCreacion(row.get("created_at", LocalDateTime.class))
                .fechaActualizacion(row.get("updated_at", LocalDateTime.class))
                .salarioBase(BigDecimal.ZERO) // Se obtendrá vía AuthService
                .deudaTotalMensualSolicitudesAprobadas(BigDecimal.ZERO) // Calculado por separado
                .documentoCliente(row.get("client_document", String.class))
                .notas(row.get("notes", String.class))
                .build();
    }

    private BigDecimal calculateInterestRate(String loanType) {
        if (loanType == null) return BigDecimal.valueOf(15.0);
        
        return switch (loanType) {
            case "PERSONAL" -> BigDecimal.valueOf(15.5);
            case "VEHICLE" -> BigDecimal.valueOf(12.8);
            case "HOME" -> BigDecimal.valueOf(9.2);
            case "BUSINESS" -> BigDecimal.valueOf(18.3);
            default -> BigDecimal.valueOf(15.0);
        };
    }
}
