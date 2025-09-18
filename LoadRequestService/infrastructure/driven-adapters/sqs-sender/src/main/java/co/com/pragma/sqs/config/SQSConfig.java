package co.com.pragma.sqs.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.time.Duration;

/**
 * Configuración para AWS SQS Client
 */
@Configuration
@Slf4j
public class SQSConfig {

    @Value("${aws.region:us-east-2}")
    private String awsRegion;
    
    @Value("${aws.access-key-id:}")
    private String awsAccessKeyId;
    
    @Value("${aws.secret-access-key:}")
    private String awsSecretAccessKey;

    @Value("${aws.sqs.connection-timeout-ms:30000}")
    private int connectionTimeoutMs;

    @Value("${aws.sqs.read-timeout-ms:60000}")
    private int readTimeoutMs;

    /**
     * Bean de SqsAsyncClient configurado para el entorno
     */
    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        String envAccessKey = System.getenv("AWS_ACCESS_KEY_ID");
        String envSecretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
       
        try {
            // Usar credenciales explícitas si están disponibles, sino DefaultCredentialsProvider
            if (awsAccessKeyId != null && !awsAccessKeyId.isEmpty() && 
                awsSecretAccessKey != null && !awsSecretAccessKey.isEmpty()) {
                log.info("Usando credenciales explícitas desde Spring Boot configuration");
                AwsBasicCredentials awsCreds = AwsBasicCredentials.create(awsAccessKeyId, awsSecretAccessKey);
                
                       return SqsAsyncClient.builder()
                               .region(Region.of(awsRegion))
                               .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                               .overrideConfiguration(builder -> builder
                                       .apiCallTimeout(Duration.ofMillis(readTimeoutMs))
                                       .apiCallAttemptTimeout(Duration.ofMillis(connectionTimeoutMs))
                                       .retryPolicy(retryPolicy -> retryPolicy.numRetries(3)))
                               .build();
            } else {
                log.info("Usando DefaultCredentialsProvider (variables de entorno, perfil AWS, etc.)");
                
                return SqsAsyncClient.builder()
                        .region(Region.of(awsRegion))
                        .credentialsProvider(DefaultCredentialsProvider.create())
                        .overrideConfiguration(builder -> builder
                                .apiCallTimeout(Duration.ofMillis(readTimeoutMs))
                                .apiCallAttemptTimeout(Duration.ofMillis(connectionTimeoutMs))
                                .retryPolicy(retryPolicy -> retryPolicy.numRetries(3)))
                        .build();
            }
            
        } catch (Exception e) {
            log.error("❌ Error configurando SqsAsyncClient: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo configurar SqsAsyncClient", e);
        } finally {
            log.info("✅ SqsAsyncClient configurado exitosamente");
        }
    }
}

