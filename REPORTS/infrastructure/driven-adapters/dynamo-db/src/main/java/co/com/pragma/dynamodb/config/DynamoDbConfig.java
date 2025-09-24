package co.com.pragma.dynamodb.config;

import co.com.pragma.dynamodb.LoanReportDynamoRepository;
import co.com.pragma.usecase.report.gateways.LoanReportRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

@Configuration
public class DynamoDbConfig {
    
    @Value("${aws.dynamodb.region:us-east-1}")
    private String region;
    
    @Value("${aws.dynamodb.table.loan-reports:loan-reports}")
    private String tableName;
    
    @Value("${aws.access-key-id:}")
    private String accessKeyId;
    
    @Value("${aws.secret-access-key:}")
    private String secretAccessKey;
    
    @Bean
    public DynamoDbAsyncClient dynamoDbAsyncClient() {
        System.out.println("🔧 DynamoDB Config - Region: " + region);
        System.out.println("🔧 DynamoDB Config - AccessKey: " + accessKeyId);
        
        return DynamoDbAsyncClient.builder()
                .region(Region.of(region))
                .credentialsProvider(software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                        software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .build();
    }
    
    @Bean
    public LoanReportRepository loanReportRepository(DynamoDbAsyncClient dynamoDbClient) {
        return new LoanReportDynamoRepository(dynamoDbClient, tableName);
    }
}