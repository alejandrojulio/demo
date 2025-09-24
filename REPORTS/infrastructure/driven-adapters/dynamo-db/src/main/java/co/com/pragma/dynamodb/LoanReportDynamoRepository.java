package co.com.pragma.dynamodb;

import co.com.pragma.model.report.LoanReport;
import co.com.pragma.usecase.report.gateways.LoanReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public class LoanReportDynamoRepository implements LoanReportRepository {
    
    private final DynamoDbAsyncClient dynamoDbClient;
    private final String tableName;
    
    private static final String REPORT_ID = "loan-report";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    @Override
    public Mono<LoanReport> findReport() {
        log.debug("Consultando reporte en DynamoDB - ID: {}", REPORT_ID);
        
        Map<String, AttributeValue> key = Map.of(
                "id", AttributeValue.builder().s(REPORT_ID).build()
        );
        
        GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build();
        
        return Mono.fromFuture(dynamoDbClient.getItem(request))
                .map(GetItemResponse::item)
                .filter(item -> !item.isEmpty())
                .map(this::mapToLoanReport)
                .doOnNext(report -> log.debug("Reporte encontrado: {}", report))
                .onErrorResume(Exception.class, error -> {
                    log.error("Error al consultar reporte en DynamoDB", error);
                    return Mono.empty();
                });
    }
    
    @Override
    public Mono<LoanReport> saveReport(LoanReport report) {
        log.debug("Guardando reporte en DynamoDB: {}", report);
        
        Map<String, AttributeValue> item = mapToAttributeValueMap(report);
        
        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();
        
        return Mono.fromFuture(dynamoDbClient.putItem(request))
                .thenReturn(report)
                .doOnNext(savedReport -> log.debug("Reporte guardado exitosamente: {}", savedReport))
                .onErrorResume(Exception.class, error -> {
                    log.error("Error al guardar reporte en DynamoDB", error);
                    return Mono.error(new RuntimeException("Error al guardar reporte", error));
                });
    }
    
    @Override
    public Mono<LoanReport> updateCounter(String loanId) {
        return updateCounter(loanId, "MANUAL");
    }
    
    @Override
    public Mono<LoanReport> updateCounter(String loanId, String approvalType) {
        return updateCounter(loanId, approvalType, java.math.BigDecimal.ZERO);
    }
    
    @Override
    public Mono<LoanReport> updateCounter(String loanId, String approvalType, java.math.BigDecimal approvedAmount) {
        log.debug("Actualizando contador para préstamo: {} - Tipo: {} - Monto: ${}", loanId, approvalType, approvedAmount);
        
        Map<String, AttributeValue> key = Map.of(
                "id", AttributeValue.builder().s(REPORT_ID).build()
        );
        
        boolean isManual = "MANUAL".equalsIgnoreCase(approvalType);
        
        java.math.BigDecimal amount = approvedAmount != null ? approvedAmount : java.math.BigDecimal.ZERO;
        
        Map<String, AttributeValue> expressionAttributeValues = Map.of(
                ":inc", AttributeValue.builder().n("1").build(),
                ":amountInc", AttributeValue.builder().n(amount.toString()).build(),
                ":loanId", AttributeValue.builder().s(loanId).build(),
                ":now", AttributeValue.builder().s(LocalDateTime.now().format(FORMATTER)).build(),
                ":defaultValue", AttributeValue.builder().n("0").build()
        );
        
        Map<String, String> expressionAttributeNames = Map.of(
                "#total", "totalApprovedLoans",
                "#manual", "manualApprovedLoans",
                "#automatic", "automaticApprovedLoans",
                "#totalAmount", "totalApprovedAmount",
                "#manualAmount", "manualApprovedAmount",
                "#automaticAmount", "automaticApprovedAmount",
                "#lastLoanId", "lastLoanId",
                "#lastUpdated", "lastUpdated"
        );
        
        String updateExpression = isManual 
            ? "SET #total = if_not_exists(#total, :defaultValue) + :inc, " +
              "#manual = if_not_exists(#manual, :defaultValue) + :inc, " +
              "#automatic = if_not_exists(#automatic, :defaultValue), " +
              "#totalAmount = if_not_exists(#totalAmount, :defaultValue) + :amountInc, " +
              "#manualAmount = if_not_exists(#manualAmount, :defaultValue) + :amountInc, " +
              "#automaticAmount = if_not_exists(#automaticAmount, :defaultValue), " +
              "#lastLoanId = :loanId, #lastUpdated = :now"
            : "SET #total = if_not_exists(#total, :defaultValue) + :inc, " +
              "#manual = if_not_exists(#manual, :defaultValue), " +
              "#automatic = if_not_exists(#automatic, :defaultValue) + :inc, " +
              "#totalAmount = if_not_exists(#totalAmount, :defaultValue) + :amountInc, " +
              "#manualAmount = if_not_exists(#manualAmount, :defaultValue), " +
              "#automaticAmount = if_not_exists(#automaticAmount, :defaultValue) + :amountInc, " +
              "#lastLoanId = :loanId, #lastUpdated = :now";
        
        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .updateExpression(updateExpression)
                .expressionAttributeValues(expressionAttributeValues)
                .expressionAttributeNames(expressionAttributeNames)
                .returnValues(ReturnValue.ALL_NEW)
                .build();
        
        return Mono.fromFuture(dynamoDbClient.updateItem(request))
                .map(UpdateItemResponse::attributes)
                .map(this::mapToLoanReport)
                .doOnNext(updatedReport -> log.debug("Contador actualizado: Total={}, Manual={}, Automático={}, Monto Total=${}, Monto Manual=${}, Monto Automático=${}", 
                        updatedReport.getTotalApprovedLoans(), 
                        updatedReport.getManualApprovedLoans(),
                        updatedReport.getAutomaticApprovedLoans(),
                        updatedReport.getTotalApprovedAmount(),
                        updatedReport.getManualApprovedAmount(),
                        updatedReport.getAutomaticApprovedAmount()))
                .onErrorResume(Exception.class, error -> {
                    log.error("Error al actualizar contador en DynamoDB", error);
                    return Mono.error(new RuntimeException("Error al actualizar contador", error));
                });
    }
    
    private LoanReport mapToLoanReport(Map<String, AttributeValue> item) {
        if (item == null || item.isEmpty()) {
            return null;
        }
        
        return LoanReport.builder()
                .id(getStringValue(item, "id"))
                .totalApprovedLoans(getLongValue(item, "totalApprovedLoans"))
                .manualApprovedLoans(getLongValue(item, "manualApprovedLoans"))
                .automaticApprovedLoans(getLongValue(item, "automaticApprovedLoans"))
                .totalApprovedAmount(getBigDecimalValue(item, "totalApprovedAmount"))
                .manualApprovedAmount(getBigDecimalValue(item, "manualApprovedAmount"))
                .automaticApprovedAmount(getBigDecimalValue(item, "automaticApprovedAmount"))
                .lastUpdated(getLocalDateTimeValue(item, "lastUpdated"))
                .lastLoanId(getStringValue(item, "lastLoanId"))
                .build();
    }
    
    private Map<String, AttributeValue> mapToAttributeValueMap(LoanReport report) {
        Map<String, AttributeValue> item = new HashMap<>();
        
        item.put("id", AttributeValue.builder().s(report.getId()).build());
        item.put("totalApprovedLoans", AttributeValue.builder().n(report.getTotalApprovedLoans().toString()).build());
        item.put("manualApprovedLoans", AttributeValue.builder().n(report.getManualApprovedLoans().toString()).build());
        item.put("automaticApprovedLoans", AttributeValue.builder().n(report.getAutomaticApprovedLoans().toString()).build());
        item.put("totalApprovedAmount", AttributeValue.builder().n(report.getTotalApprovedAmount().toString()).build());
        item.put("manualApprovedAmount", AttributeValue.builder().n(report.getManualApprovedAmount().toString()).build());
        item.put("automaticApprovedAmount", AttributeValue.builder().n(report.getAutomaticApprovedAmount().toString()).build());
        item.put("lastUpdated", AttributeValue.builder().s(report.getLastUpdated().format(FORMATTER)).build());
        
        if (report.getLastLoanId() != null) {
            item.put("lastLoanId", AttributeValue.builder().s(report.getLastLoanId()).build());
        }
        
        return item;
    }
    
    private String getStringValue(Map<String, AttributeValue> item, String key) {
        AttributeValue value = item.get(key);
        return value != null ? value.s() : null;
    }
    
    private Long getLongValue(Map<String, AttributeValue> item, String key) {
        AttributeValue value = item.get(key);
        return value != null ? Long.valueOf(value.n()) : 0L;
    }
    
    private java.math.BigDecimal getBigDecimalValue(Map<String, AttributeValue> item, String key) {
        AttributeValue value = item.get(key);
        if (value != null && value.n() != null) {
            return new java.math.BigDecimal(value.n());
        }
        return java.math.BigDecimal.ZERO;
    }
    
    private LocalDateTime getLocalDateTimeValue(Map<String, AttributeValue> item, String key) {
        AttributeValue value = item.get(key);
        return value != null ? LocalDateTime.parse(value.s(), FORMATTER) : LocalDateTime.now();
    }
}
