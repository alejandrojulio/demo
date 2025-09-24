package co.com.pragma.model.report;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class LoanReport {
    private String id;
    private Long totalApprovedLoans;
    private Long manualApprovedLoans;      // Contador de aprobaciones manuales
    private Long automaticApprovedLoans;   // Contador de aprobaciones automáticas
    private java.math.BigDecimal totalApprovedAmount;      // Monto total de préstamos aprobados
    private java.math.BigDecimal manualApprovedAmount;     // Monto total de aprobaciones manuales
    private java.math.BigDecimal automaticApprovedAmount;  // Monto total de aprobaciones automáticas
    private LocalDateTime lastUpdated;
    private String lastLoanId;
    
    // Constructor para crear un nuevo reporte
    public static LoanReport createNew() {
        return LoanReport.builder()
                .id("loan-report")
                .totalApprovedLoans(0L)
                .manualApprovedLoans(0L)
                .automaticApprovedLoans(0L)
                .totalApprovedAmount(java.math.BigDecimal.ZERO)
                .manualApprovedAmount(java.math.BigDecimal.ZERO)
                .automaticApprovedAmount(java.math.BigDecimal.ZERO)
                .lastUpdated(LocalDateTime.now())
                .build();
    }
    
    // Método para incrementar el contador según el tipo
    public LoanReport incrementCounter(String loanId, String approvalType) {
        return incrementCounter(loanId, approvalType, java.math.BigDecimal.ZERO);
    }
    
    // Método para incrementar contador y monto según el tipo
    public LoanReport incrementCounter(String loanId, String approvalType, java.math.BigDecimal approvedAmount) {
        boolean isManual = "MANUAL".equalsIgnoreCase(approvalType);
        java.math.BigDecimal amount = approvedAmount != null ? approvedAmount : java.math.BigDecimal.ZERO;
        
        return this.toBuilder()
                .totalApprovedLoans(this.totalApprovedLoans + 1)
                .manualApprovedLoans(isManual ? this.manualApprovedLoans + 1 : this.manualApprovedLoans)
                .automaticApprovedLoans(!isManual ? this.automaticApprovedLoans + 1 : this.automaticApprovedLoans)
                .totalApprovedAmount(this.totalApprovedAmount.add(amount))
                .manualApprovedAmount(isManual ? this.manualApprovedAmount.add(amount) : this.manualApprovedAmount)
                .automaticApprovedAmount(!isManual ? this.automaticApprovedAmount.add(amount) : this.automaticApprovedAmount)
                .lastUpdated(LocalDateTime.now())
                .lastLoanId(loanId)
                .build();
    }
    
    // Método para compatibilidad (asume manual si no se especifica)
    public LoanReport incrementCounter(String loanId) {
        return incrementCounter(loanId, "MANUAL");
    }
    
    public boolean isInitialized() {
        return totalApprovedLoans != null;
    }
}
