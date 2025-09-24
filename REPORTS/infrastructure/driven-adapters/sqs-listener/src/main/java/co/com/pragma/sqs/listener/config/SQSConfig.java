package co.com.pragma.sqs.listener.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.net.URI;

@Configuration
@EnableAsync
@EnableScheduling
public class SQSConfig {
    
    @Value("${aws.sqs.region:us-east-1}")
    private String region;
    
    @Value("${aws.sqs.endpoint:}")
    private String endpoint;
    
    @Value("${aws.access-key-id:test}")
    private String accessKeyId;
    
    @Value("${aws.secret-access-key:test}")
    private String secretAccessKey;
    
    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        System.out.println("🔧 SQS Config - Region: " + region);
        System.out.println("🔧 SQS Config - AccessKey: " + accessKeyId);
        System.out.println("🔧 SQS Config - SecretKey: " + (secretAccessKey != null ? secretAccessKey.substring(0, 4) + "****" : "null"));
        
        var builder = SqsAsyncClient.builder()
                .region(Region.of(region));
        
        // Usar siempre las credenciales configuradas en application.yaml
        builder.credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKeyId, secretAccessKey)));
        
        return builder.build();
    }
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
