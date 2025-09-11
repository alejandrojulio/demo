package co.com.pragma.sqs;

import co.com.pragma.model.notification.NotificationMessage;
import co.com.pragma.model.notification.gateways.NotificationGateway;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Adaptador para envío de notificaciones via SQS
 * Envía mensajes reales a AWS SQS
 */
@Component
public class SqsNotificationAdapter implements NotificationGateway {
    
    private final String queueUrl;
    private final ObjectMapper objectMapper;
    private final AmazonSQS sqsClient;
    
    public SqsNotificationAdapter(@Value("${aws.sqs.loan-decisions-queue-url}") String queueUrl,
                                 @Value("${aws.region}") String region,
                                 @Value("${aws.access-key-id}") String accessKeyId,
                                 @Value("${aws.secret-access-key}") String secretAccessKey) {
        this.queueUrl = queueUrl;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        // Crear credenciales AWS
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKeyId, secretAccessKey);
        
        // Crear cliente SQS con credenciales
        this.sqsClient = AmazonSQSClientBuilder.standard()
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .build();
        
        System.out.println("🔑 Cliente SQS configurado para región: " + region);
        System.out.println("🔗 Queue URL: " + queueUrl);
    }
    
    @Override
    public Mono<Void> sendNotification(NotificationMessage message) {
        return Mono.fromCallable(() -> {
            try {
                // Convertir el mensaje a JSON
                String messageBody = objectMapper.writeValueAsString(message);
                
                // Logear información del envío
                System.out.println("🚀 ENVIANDO A SQS: " + queueUrl);
                System.out.println("📧 MENSAJE: " + messageBody);
                System.out.println("   Tipo: " + message.getEventType());
                System.out.println("   Cliente: " + message.getClientEmail());
                System.out.println("   Solicitud: " + message.getSolicitudId());
                System.out.println("   Decisión: " + message.getDecision());
                
                // Enviar mensaje real a SQS
                SendMessageRequest sendMessageRequest = new SendMessageRequest()
                        .withQueueUrl(queueUrl)
                        .withMessageBody(messageBody);
                
                var result = sqsClient.sendMessage(sendMessageRequest);
                System.out.println("✅ MENSAJE ENVIADO A SQS - MessageId: " + result.getMessageId());
                
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Error al procesar notificación: " + e.getMessage(), e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic()) // Ejecutar en hilo separado
        .then(); // Convertir a Mono<Void>
    }
}
