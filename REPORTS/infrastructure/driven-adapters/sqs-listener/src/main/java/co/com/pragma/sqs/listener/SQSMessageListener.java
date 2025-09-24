package co.com.pragma.sqs.listener;

import co.com.pragma.model.common.MessageFormatter;
import co.com.pragma.model.common.Messages;
import co.com.pragma.model.loan.LoanApprovedEvent;
import co.com.pragma.usecase.report.ProcessLoanApprovedEventUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import jakarta.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
@Slf4j
@EnableAsync
public class SQSMessageListener {
    
    private final ProcessLoanApprovedEventUseCase processLoanApprovedEventUseCase;
    private final ObjectMapper objectMapper;
    private final SqsAsyncClient sqsAsyncClient;
    
    @Value("${aws.sqs.queue.loan-approved:loan-approved-queue}")
    private String queueName;
    
    @Value("${aws.sqs.region:us-east-1}")
    private String sqsRegion;
    
    @Value("${aws.account-id:}")
    private String awsAccountId;
    
    @PostConstruct
    public void init() {
        log.info("🔧 SQS Listener configurado para cola: {}", queueName);
        log.info("🔧 SQS Region: {}", sqsRegion);
        log.info("🔗 URL completa de cola: {}", getQueueUrl());
    }
    
    @Scheduled(fixedDelay = 5000) // Polling cada 5 segundos
    @Async
    public void pollMessages() {
        try {
            ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(getQueueUrl())
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(20) // Long polling
                    .build();
            
            sqsAsyncClient.receiveMessage(receiveRequest)
                    .whenComplete((response, error) -> {
                        if (error != null) {
                            log.warn("Error al recibir mensajes SQS: {}", error.getMessage());
                            return;
                        }
                        
                        response.messages().forEach(this::processMessage);
                    });
                    
        } catch (Exception e) {
            log.error("Error en polling de SQS", e);
        }
    }
    
    private void processMessage(Message message) {
        log.info("📨 Mensaje SQS recibido: {}", message.body());
        
        try {
            // Parsear el mensaje JSON
            LoanApprovedEvent event = objectMapper.readValue(message.body(), LoanApprovedEvent.class);
            
            log.info(MessageFormatter.format(Messages.LOG_EVENT_RECEIVED, 
                    event.getEventType(), event.getLoanId()));
            
            // Procesar el evento de forma reactiva
            processLoanApprovedEventUseCase.processLoanApprovedEvent(event)
                    .doOnNext(updatedReport -> {
                        log.info(MessageFormatter.format(Messages.LOG_COUNTER_UPDATED, 
                                updatedReport.getTotalApprovedLoans(), 
                                updatedReport.getLastLoanId()));
                    })
                    .doOnSuccess(result -> {
                        // Eliminar mensaje de la cola solo si se procesó exitosamente
                        deleteMessage(message);
                    })
                    .doOnError(error -> {
                        log.error("Error al procesar evento de préstamo aprobado", error);
                    })
                    .onErrorResume(error -> {
                        log.error("Error procesando mensaje, no se eliminará de la cola", error);
                        return Mono.empty();
                    })
                    .subscribe(); // Subscribe para ejecutar la cadena reactiva
            
        } catch (Exception e) {
            log.error("Error al parsear mensaje SQS: {}", message.body(), e);
        }
    }
    
    private void deleteMessage(Message message) {
        DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                .queueUrl(getQueueUrl())
                .receiptHandle(message.receiptHandle())
                .build();
        
        sqsAsyncClient.deleteMessage(deleteRequest)
                .whenComplete((response, error) -> {
                    if (error != null) {
                        log.error("Error eliminando mensaje SQS", error);
                    } else {
                        log.debug("✅ Mensaje SQS eliminado exitosamente");
                    }
                });
    }
    
    private String getQueueUrl() {
        if (awsAccountId != null && !awsAccountId.trim().isEmpty()) {
            // URL completa de SQS con account ID
            String url = String.format("https://sqs.%s.amazonaws.com/%s/%s", 
                    sqsRegion, awsAccountId, queueName);
            log.debug("🔗 Usando URL de cola AWS: {}", url);
            return url;
        } else {
            // Usar GetQueueUrl API para obtener la URL automáticamente
            try {
                var getQueueUrlRequest = software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest.builder()
                        .queueName(queueName)
                        .build();
                
                var response = sqsAsyncClient.getQueueUrl(getQueueUrlRequest).get();
                String url = response.queueUrl();
                log.debug("🔗 URL de cola obtenida automáticamente: {}", url);
                return url;
            } catch (Exception e) {
                log.error("Error obteniendo URL de cola SQS", e);
                // Fallback URL - necesitarás ajustar el account ID
                String fallbackUrl = String.format("https://sqs.%s.amazonaws.com/123456789/%s", 
                        sqsRegion, queueName);
                log.warn("⚠️ Usando URL fallback: {}", fallbackUrl);
                return fallbackUrl;
            }
        }
    }
}
