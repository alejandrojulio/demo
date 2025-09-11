package co.com.pragma.usecase.loan;

import co.com.pragma.model.common.MessageFormatter;
import co.com.pragma.model.common.Messages;
import co.com.pragma.model.loan.LoanRequest;
import co.com.pragma.model.loan.LoanRequestDTO;
import co.com.pragma.model.loan.gateways.LoanApplicationLogger;
import co.com.pragma.model.loan.gateways.LoanRequestRepository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LoanRequestUseCase {

    private final LoanRequestRepository loanRequestRepository;
    private final LoanApplicationLogger logger;

    private static final BigDecimal MIN_LOAN_AMOUNT = new BigDecimal("100000");
    private static final BigDecimal MAX_LOAN_AMOUNT = new BigDecimal("50000000");
    private static final Integer MIN_LOAN_TERM = 1;
    private static final Integer MAX_LOAN_TERM = 120;

    public LoanRequestUseCase(LoanRequestRepository loanRequestRepository, 
                             LoanApplicationLogger logger) {
        this.loanRequestRepository = loanRequestRepository;
        this.logger = logger;
    }

    @Transactional
    public Mono<LoanRequest> createLoanRequest(LoanRequestDTO loanRequestDTO) {
        logger.info(MessageFormatter.format(Messages.LOG_CREATING_LOAN_REQUEST, loanRequestDTO.getClientDocumentId()));

        return Mono.just(loanRequestDTO)
                .doOnNext(dto -> logger.info(MessageFormatter.format(Messages.LOG_VALIDATING_LOAN_DATA, dto.getClientDocumentId())))
                .flatMap(this::validateRequiredFields)
                .flatMap(this::validateLoanAmount)
                .flatMap(this::validateLoanTerm)
                .flatMap(this::validateLoanType)
                .flatMap(this::checkExistingLoan)
                .map(this::enrichLoanRequestData)
                .doOnNext(loanRequest -> logger.info(MessageFormatter.format(Messages.LOG_SAVING_LOAN_REQUEST, loanRequest.getClientDocumentId())))
                .flatMap(loanRequestRepository::save)
                .doOnSuccess(savedLoanRequest -> logger.info(MessageFormatter.format(Messages.LOG_LOAN_REQUEST_CREATED_SUCCESS, savedLoanRequest.getId())))
                .doOnError(error -> logger.error(MessageFormatter.format(Messages.LOG_ERROR_CREATING_LOAN_REQUEST, loanRequestDTO.getClientDocumentId()), error));
    }

    private Mono<LoanRequestDTO> validateRequiredFields(LoanRequestDTO dto) {
        if (dto.getClientDocumentId() == null || dto.getClientDocumentId().trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException(Messages.LOAN_CLIENT_DOCUMENT_REQUIRED));
        }

        if (dto.getAmount() == null) {
            return Mono.error(new IllegalArgumentException(Messages.LOAN_AMOUNT_REQUIRED));
        }

        if (dto.getTermInMonths() == null) {
            return Mono.error(new IllegalArgumentException(Messages.LOAN_TERM_REQUIRED));
        }

        if (dto.getLoanType() == null) {
            return Mono.error(new IllegalArgumentException(Messages.LOAN_TYPE_REQUIRED));
        }

        return Mono.just(dto);
    }

    private void validateClientExists(LoanRequestDTO dto) { //Mono<LoanRequestDTO>
        logger.info(MessageFormatter.format(Messages.LOG_VALIDATING_CLIENT_EXISTS, dto.getClientDocumentId()));


    }

    private Mono<LoanRequestDTO> validateLoanAmount(LoanRequestDTO dto) {
        logger.info(MessageFormatter.format(Messages.LOG_VALIDATING_LOAN_AMOUNT, dto.getAmount(), dto.getClientDocumentId()));

        if (dto.getAmount().compareTo(MIN_LOAN_AMOUNT) < 0 || dto.getAmount().compareTo(MAX_LOAN_AMOUNT) > 0) {
            return Mono.error(new IllegalArgumentException(Messages.ERROR_LOAN_AMOUNT_INVALID));
        }

        logger.info(MessageFormatter.format(Messages.LOG_LOAN_AMOUNT_VALID, dto.getClientDocumentId()));
        return Mono.just(dto);
    }

    private Mono<LoanRequestDTO> validateLoanTerm(LoanRequestDTO dto) {
        logger.info(MessageFormatter.format(Messages.LOG_VALIDATING_LOAN_TERM, dto.getTermInMonths(), dto.getClientDocumentId()));

        if (dto.getTermInMonths() < MIN_LOAN_TERM || dto.getTermInMonths() > MAX_LOAN_TERM) {
            return Mono.error(new IllegalArgumentException(Messages.ERROR_LOAN_TERM_INVALID));
        }

        logger.info(MessageFormatter.format(Messages.LOG_LOAN_TERM_VALID, dto.getClientDocumentId()));
        return Mono.just(dto);
    }

    private Mono<LoanRequestDTO> validateLoanType(LoanRequestDTO dto) {
        logger.info(MessageFormatter.format(Messages.LOG_VALIDATING_LOAN_TYPE, dto.getLoanType(), dto.getClientDocumentId()));

        try {
            LoanRequest.LoanType.valueOf(dto.getLoanType().name());
            logger.info(MessageFormatter.format(Messages.LOG_LOAN_TYPE_VALID, dto.getClientDocumentId()));
            return Mono.just(dto);
        } catch (IllegalArgumentException e) {
            return Mono.error(new IllegalArgumentException(
                    MessageFormatter.format(Messages.ERROR_LOAN_TYPE_INVALID, dto.getLoanType())
            ));
        }
    }

    private Mono<LoanRequestDTO> checkExistingLoan(LoanRequestDTO dto) {
        logger.info(MessageFormatter.format(Messages.LOG_CHECKING_EXISTING_LOAN, dto.getClientDocumentId(), dto.getLoanType()));

        return loanRequestRepository.existsByClientDocumentAndTypeAndPendingStatus(dto.getClientDocumentId(), dto.getLoanType())
                .flatMap(exists -> {
                    if (exists) {
                        logger.warn(MessageFormatter.format(Messages.LOG_EXISTING_LOAN_FOUND, dto.getClientDocumentId(), dto.getLoanType()));
                        return Mono.error(new IllegalArgumentException(
                                MessageFormatter.format(Messages.LOAN_VALIDATION_EXISTING, dto.getLoanType().getDisplayName())
                        ));
                    } else {
                        logger.info(MessageFormatter.format(Messages.LOG_NO_EXISTING_LOAN, dto.getClientDocumentId(), dto.getLoanType()));
                        return Mono.just(dto);
                    }
                });
    }

    private LoanRequest enrichLoanRequestData(LoanRequestDTO dto) {
        logger.info(MessageFormatter.format(Messages.LOG_ENRICHING_LOAN_DATA, dto.getClientDocumentId()));

        LocalDateTime now = LocalDateTime.now();
        
        return LoanRequest.builder()
                .clientDocumentId(dto.getClientDocumentId().trim())
                .amount(dto.getAmount())
                .termInMonths(dto.getTermInMonths())
                .loanType(dto.getLoanType())
                .status(LoanRequest.LoanStatus.PENDING_REVIEW)
                .createdAt(now)
                .updatedAt(now)
                .notes(dto.getNotes() != null ? dto.getNotes().trim() : null)
                .build();
    }

    @Transactional
    public Mono<LoanRequest> updateLoanRequest(String loanRequestId, LoanRequestDTO loanRequestDTO) {
        logger.info("Iniciando actualización de solicitud de préstamo con ID: {}", loanRequestId);
        
        return loanRequestRepository.findById(Long.valueOf(loanRequestId))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Solicitud de préstamo no encontrada con ID: " + loanRequestId)))
                .flatMap(existingLoanRequest -> {
                    LoanRequest updatedLoanRequest = existingLoanRequest.toBuilder()
                            .updatedAt(LocalDateTime.now())
                            .notes(loanRequestDTO.getNotes() != null ? loanRequestDTO.getNotes().trim() : existingLoanRequest.getNotes())
                            .build();
                    
                    logger.info("Actualizando solicitud de préstamo para cliente: {}", updatedLoanRequest.getClientDocumentId());
                    return loanRequestRepository.update(updatedLoanRequest);
                })
                .doOnSuccess(updatedLoanRequest -> logger.info("Solicitud actualizada exitosamente con ID: {}", updatedLoanRequest.getId()))
                .doOnError(error -> logger.error("Error actualizando solicitud con ID: " + loanRequestId, error));
    }

    public Flux<LoanRequest> getAllLoanRequests() {
        logger.info("Obteniendo todas las solicitudes de préstamo");
        
        return loanRequestRepository.findAll()
                .doOnNext(loanRequest -> logger.warn("Solicitud encontrada: {} para cliente: {}", "test"))
                .doOnComplete(() -> logger.info("Consulta de todas las solicitudes completada"))
                .doOnError(error -> logger.error("Error obteniendo todas las solicitudes", error));
    }
}
