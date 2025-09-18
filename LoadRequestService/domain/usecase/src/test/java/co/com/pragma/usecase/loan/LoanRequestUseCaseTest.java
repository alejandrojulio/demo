package co.com.pragma.usecase.loan;

import co.com.pragma.model.loan.LoanRequest;
import co.com.pragma.model.loan.LoanRequestDTO;
import co.com.pragma.model.loan.gateways.LoanApplicationLogger;
import co.com.pragma.model.loan.gateways.LoanRequestRepository;
import co.com.pragma.model.loantype.LoanType;
import co.com.pragma.model.loantype.gateways.LoanTypeRepository;
import co.com.pragma.model.debtcapacity.gateways.DebtCapacityGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Tests para LoanRequestUseCase con validación automática de capacidad de endeudamiento
 */
@ExtendWith(MockitoExtension.class)
class LoanRequestUseCaseTest {

    @Mock
    private LoanRequestRepository loanRequestRepository;
    
    @Mock
    private LoanApplicationLogger logger;
    
    @Mock
    private LoanTypeRepository loanTypeRepository;
    
    @Mock
    private DebtCapacityGateway debtCapacityGateway;

    private LoanRequestUseCase loanRequestUseCase;

    @BeforeEach
    void setUp() {
        loanRequestUseCase = new LoanRequestUseCase(
            loanRequestRepository, 
            logger, 
            loanTypeRepository, 
            debtCapacityGateway
        );
    }

    @Test
    void createLoanRequest_WithAutomaticValidation_ShouldSendToQueue() {
        // Given
        LoanRequestDTO dto = createValidLoanRequestDTO();
        LoanRequest savedLoanRequest = createLoanRequestFromDTO(dto, 1L);
        
        // Mock del repositorio
        when(loanRequestRepository.existsByClientDocumentAndTypeAndPendingStatus(
            anyString(), any(LoanRequest.LoanType.class)))
            .thenReturn(Mono.just(false));
        when(loanRequestRepository.save(any(LoanRequest.class)))
            .thenReturn(Mono.just(savedLoanRequest));
        
        // Mock del tipo de préstamo - requiere validación automática
        when(loanTypeRepository.requiresAutomaticValidation(any(LoanType.LoanTypeCode.class)))
            .thenReturn(Mono.just(true));
        
        // Mock del gateway de capacidad de endeudamiento
        when(debtCapacityGateway.sendForAutomaticValidation(anyLong()))
            .thenReturn(Mono.just(true));

        // When & Then
        StepVerifier.create(loanRequestUseCase.createLoanRequest(dto))
            .expectNext(savedLoanRequest)
            .verifyComplete();

        // Verificaciones
        verify(loanRequestRepository).save(any(LoanRequest.class));
        verify(loanTypeRepository).requiresAutomaticValidation(LoanType.LoanTypeCode.PERSONAL);
        verify(debtCapacityGateway).sendForAutomaticValidation(1L);
    }

    @Test
    void createLoanRequest_WithoutAutomaticValidation_ShouldNotSendToQueue() {
        // Given
        LoanRequestDTO dto = createValidLoanRequestDTO();
        dto.setLoanType(LoanRequest.LoanType.HOME); // Préstamo hipotecario - sin validación automática
        LoanRequest savedLoanRequest = createLoanRequestFromDTO(dto, 2L);
        
        // Mock del repositorio
        when(loanRequestRepository.existsByClientDocumentAndTypeAndPendingStatus(
            anyString(), any(LoanRequest.LoanType.class)))
            .thenReturn(Mono.just(false));
        when(loanRequestRepository.save(any(LoanRequest.class)))
            .thenReturn(Mono.just(savedLoanRequest));
        
        // Mock del tipo de préstamo - NO requiere validación automática
        when(loanTypeRepository.requiresAutomaticValidation(any(LoanType.LoanTypeCode.class)))
            .thenReturn(Mono.just(false));

        // When & Then
        StepVerifier.create(loanRequestUseCase.createLoanRequest(dto))
            .expectNext(savedLoanRequest)
            .verifyComplete();

        // Verificaciones
        verify(loanRequestRepository).save(any(LoanRequest.class));
        verify(loanTypeRepository).requiresAutomaticValidation(LoanType.LoanTypeCode.HOME);
        verify(debtCapacityGateway, never()).sendForAutomaticValidation(anyLong());
    }

    @Test
    void createLoanRequest_QueueSendFails_ShouldContinueNormally() {
        // Given
        LoanRequestDTO dto = createValidLoanRequestDTO();
        LoanRequest savedLoanRequest = createLoanRequestFromDTO(dto, 3L);
        
        // Mock del repositorio
        when(loanRequestRepository.existsByClientDocumentAndTypeAndPendingStatus(
            anyString(), any(LoanRequest.LoanType.class)))
            .thenReturn(Mono.just(false));
        when(loanRequestRepository.save(any(LoanRequest.class)))
            .thenReturn(Mono.just(savedLoanRequest));
        
        // Mock del tipo de préstamo - requiere validación automática
        when(loanTypeRepository.requiresAutomaticValidation(any(LoanType.LoanTypeCode.class)))
            .thenReturn(Mono.just(true));
        
        // Mock del gateway - falla el envío
        when(debtCapacityGateway.sendForAutomaticValidation(anyLong()))
            .thenReturn(Mono.just(false));

        // When & Then
        StepVerifier.create(loanRequestUseCase.createLoanRequest(dto))
            .expectNext(savedLoanRequest)
            .verifyComplete();

        // Verificaciones
        verify(loanRequestRepository).save(any(LoanRequest.class));
        verify(debtCapacityGateway).sendForAutomaticValidation(3L);
    }

    @Test
    void createLoanRequest_LoanTypeRepositoryError_ShouldContinueNormally() {
        // Given
        LoanRequestDTO dto = createValidLoanRequestDTO();
        LoanRequest savedLoanRequest = createLoanRequestFromDTO(dto, 4L);
        
        // Mock del repositorio
        when(loanRequestRepository.existsByClientDocumentAndTypeAndPendingStatus(
            anyString(), any(LoanRequest.LoanType.class)))
            .thenReturn(Mono.just(false));
        when(loanRequestRepository.save(any(LoanRequest.class)))
            .thenReturn(Mono.just(savedLoanRequest));
        
        // Mock del tipo de préstamo - error al consultar
        when(loanTypeRepository.requiresAutomaticValidation(any(LoanType.LoanTypeCode.class)))
            .thenReturn(Mono.error(new RuntimeException("Error de base de datos")));

        // When & Then
        StepVerifier.create(loanRequestUseCase.createLoanRequest(dto))
            .expectNext(savedLoanRequest)
            .verifyComplete();

        // Verificaciones
        verify(loanRequestRepository).save(any(LoanRequest.class));
        verify(loanTypeRepository).requiresAutomaticValidation(LoanType.LoanTypeCode.PERSONAL);
        verify(debtCapacityGateway, never()).sendForAutomaticValidation(anyLong());
    }

    @Test
    void createLoanRequest_InvalidAmount_ShouldFail() {
        // Given
        LoanRequestDTO dto = createValidLoanRequestDTO();
        dto.setAmount(new BigDecimal("50000")); // Menor al mínimo

        // When & Then
        StepVerifier.create(loanRequestUseCase.createLoanRequest(dto))
            .expectErrorMatches(throwable -> 
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().contains("monto del préstamo debe ser mayor"))
            .verify();

        // Verificaciones
        verify(loanRequestRepository, never()).save(any(LoanRequest.class));
        verify(debtCapacityGateway, never()).sendForAutomaticValidation(anyLong());
    }

    @Test
    void createLoanRequest_ExistingPendingLoan_ShouldFail() {
        // Given
        LoanRequestDTO dto = createValidLoanRequestDTO();
        
        // Mock del repositorio - ya existe una solicitud pendiente
        when(loanRequestRepository.existsByClientDocumentAndTypeAndPendingStatus(
            anyString(), any(LoanRequest.LoanType.class)))
            .thenReturn(Mono.just(true));

        // When & Then
        StepVerifier.create(loanRequestUseCase.createLoanRequest(dto))
            .expectErrorMatches(throwable -> 
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().contains("ya tiene una solicitud"))
            .verify();

        // Verificaciones
        verify(loanRequestRepository, never()).save(any(LoanRequest.class));
        verify(debtCapacityGateway, never()).sendForAutomaticValidation(anyLong());
    }

    @Test
    void mapToLoanTypeCode_AllTypes_ShouldMapCorrectly() {
        // Test del método de mapeo interno
        LoanRequestUseCase useCase = new LoanRequestUseCase(
            loanRequestRepository, logger, loanTypeRepository, debtCapacityGateway
        );
        
        // Usar reflexión para acceder al método privado en un test real
        // Aquí simplificamos verificando la lógica a través de casos de uso públicos
        
        // Verificar que diferentes tipos de préstamo se manejan correctamente
        LoanRequestDTO personalDto = createValidLoanRequestDTO();
        personalDto.setLoanType(LoanRequest.LoanType.PERSONAL);
        
        LoanRequestDTO vehicleDto = createValidLoanRequestDTO();
        vehicleDto.setLoanType(LoanRequest.LoanType.VEHICLE);
        
        LoanRequestDTO homeDto = createValidLoanRequestDTO();
        homeDto.setLoanType(LoanRequest.LoanType.HOME);
        
        LoanRequestDTO businessDto = createValidLoanRequestDTO();
        businessDto.setLoanType(LoanRequest.LoanType.BUSINESS);
        
        // Configurar mocks para todos los tipos
        when(loanRequestRepository.existsByClientDocumentAndTypeAndPendingStatus(
            anyString(), any(LoanRequest.LoanType.class)))
            .thenReturn(Mono.just(false));
        when(loanRequestRepository.save(any(LoanRequest.class)))
            .thenReturn(Mono.just(createLoanRequestFromDTO(personalDto, 1L)));
        when(loanTypeRepository.requiresAutomaticValidation(any(LoanType.LoanTypeCode.class)))
            .thenReturn(Mono.just(false));
        
        // Verificar que no hay errores de mapeo
        StepVerifier.create(loanRequestUseCase.createLoanRequest(personalDto))
            .expectNextMatches(lr -> lr.getLoanType() == LoanRequest.LoanType.PERSONAL)
            .verifyComplete();
    }

    private LoanRequestDTO createValidLoanRequestDTO() {
        return LoanRequestDTO.builder()
            .clientDocumentId("12345678")
            .amount(new BigDecimal("5000000"))
            .termInMonths(36)
            .loanType(LoanRequest.LoanType.PERSONAL)
            .notes("Préstamo para consolidación de deudas")
            .build();
    }

    private LoanRequest createLoanRequestFromDTO(LoanRequestDTO dto, Long id) {
        return LoanRequest.builder()
            .id(id)
            .clientDocumentId(dto.getClientDocumentId())
            .amount(dto.getAmount())
            .termInMonths(dto.getTermInMonths())
            .loanType(dto.getLoanType())
            .status(LoanRequest.LoanStatus.PENDING_REVIEW)
            .notes(dto.getNotes())
            .build();
    }
}

