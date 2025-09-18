package co.com.pragma.usecase.loan;

import co.com.pragma.model.common.MessageFormatter;
import co.com.pragma.model.common.Messages;
import co.com.pragma.model.loan.LoanRequest;
import co.com.pragma.model.loan.LoanRequestDTO;
import co.com.pragma.model.loan.gateways.LoanApplicationLogger;
import co.com.pragma.model.loan.gateways.LoanRequestRepository;
import co.com.pragma.model.loantype.LoanType;
import co.com.pragma.model.loantype.gateways.LoanTypeRepository;
import co.com.pragma.model.debtcapacity.gateways.DebtCapacityGateway;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LoanRequestUseCase {

    private final LoanRequestRepository loanRequestRepository;
    private final LoanApplicationLogger logger;
    private final LoanTypeRepository loanTypeRepository;
    private final DebtCapacityGateway debtCapacityGateway;

    private static final BigDecimal MIN_LOAN_AMOUNT = new BigDecimal("100000");
    private static final BigDecimal MAX_LOAN_AMOUNT = new BigDecimal("50000000");
    private static final Integer MIN_LOAN_TERM = 1;
    private static final Integer MAX_LOAN_TERM = 120;

    public LoanRequestUseCase(LoanRequestRepository loanRequestRepository, 
                             LoanApplicationLogger logger,
                             LoanTypeRepository loanTypeRepository,
                             DebtCapacityGateway debtCapacityGateway) {
        this.loanRequestRepository = loanRequestRepository;
        this.logger = logger;
        this.loanTypeRepository = loanTypeRepository;
        this.debtCapacityGateway = debtCapacityGateway;
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
                .flatMap(this::processAutomaticValidationIfRequired)
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
    
    /**
     * Procesa la validación automática si el tipo de préstamo lo requiere
     */
    private Mono<LoanRequest> processAutomaticValidationIfRequired(LoanRequest savedLoanRequest) {
        logger.info("Verificando si la solicitud {} requiere validación automática", savedLoanRequest.getId());
        
        // Convertir el enum de LoanRequest a LoanType.LoanTypeCode
        LoanType.LoanTypeCode typeCode = mapToLoanTypeCode(savedLoanRequest.getLoanType());
        
        return loanTypeRepository.requiresAutomaticValidation(typeCode)
                .flatMap(requiresValidation -> {
                    if (requiresValidation) {
                        logger.info("Solicitud {} requiere validación automática. Enviando a cola SQS", savedLoanRequest.getId());
                        return debtCapacityGateway.sendForAutomaticValidation(savedLoanRequest.getId())
                                .map(sent -> {
                                    if (sent) {
                                        logger.info("Solicitud {} enviada exitosamente para validación automática", savedLoanRequest.getId());
                                    } else {
                                        logger.warn("Error enviando solicitud {} para validación automática", savedLoanRequest.getId());
                                    }
                                    return savedLoanRequest;
                                });
                    } else {
                        logger.info("Solicitud {} no requiere validación automática. Permanece en estado PENDING_REVIEW", savedLoanRequest.getId());
                        return Mono.just(savedLoanRequest);
                    }
                })
                .onErrorResume(error -> {
                    logger.error("Error procesando validación automática para solicitud " + savedLoanRequest.getId() + ": " + error.getMessage(), error);
                    // En caso de error, devolver la solicitud sin validación automática
                    return Mono.just(savedLoanRequest);
                });
    }
    
    /**
     * Mapea el enum de LoanRequest.LoanType a LoanType.LoanTypeCode
     */
    private LoanType.LoanTypeCode mapToLoanTypeCode(LoanRequest.LoanType loanType) {
        switch (loanType) {
            case PERSONAL: return LoanType.LoanTypeCode.PERSONAL;
            case VEHICLE: return LoanType.LoanTypeCode.VEHICLE;
            case HOME: return LoanType.LoanTypeCode.HOME;
            case BUSINESS: return LoanType.LoanTypeCode.BUSINESS;
            default: throw new IllegalArgumentException("Tipo de préstamo no válido: " + loanType);
        }
    }
}
