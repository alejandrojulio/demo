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
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
        
        // Intentar cargar credenciales del archivo .env
        String finalAccessKey = accessKeyId;
        String finalSecretKey = secretAccessKey;
        
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory("../")
                    .ignoreIfMissing()
                    .load();
            
            if (dotenv.get("AWS_ACCESS_KEY_ID") != null) {
                finalAccessKey = dotenv.get("AWS_ACCESS_KEY_ID");
            }
            
            if (dotenv.get("AWS_SECRET_ACCESS_KEY") != null) {
                finalSecretKey = dotenv.get("AWS_SECRET_ACCESS_KEY");
            }
            
        } catch (Exception e) {
            System.out.println("No se pudo cargar .env");
        }
        
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(finalAccessKey, finalSecretKey);
        this.sqsClient = AmazonSQSClientBuilder.standard()
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .build();
    }
    
    @Override
    public Mono<Void> sendNotification(NotificationMessage message) {
        return Mono.fromCallable(() -> {
            try {
                String messageBody = objectMapper.writeValueAsString(message);
                System.out.println("ENVIANDO A SQS: " + queueUrl);
                System.out.println("📧 MENSAJE: " + messageBody);
                SendMessageRequest sendMessageRequest = new SendMessageRequest()
                        .withQueueUrl(queueUrl)
                        .withMessageBody(messageBody);
                
                var result = sqsClient.sendMessage(sendMessageRequest);
                System.out.println("MENSAJE ENVIADO A SQS - MessageId: " + result.getMessageId());
                
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Error al procesar notificación: " + e.getMessage(), e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
}
