package co.com.pragma.api.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanReportDTO {
    private Long totalApprovedLoans;
    private Long manualApprovedLoans;
    private Long automaticApprovedLoans;
    private java.math.BigDecimal totalApprovedAmount;
    private java.math.BigDecimal manualApprovedAmount;
    private java.math.BigDecimal automaticApprovedAmount;
    private LocalDateTime lastUpdated;
    private String lastLoanId;
    private String status;
    
    public static LoanReportDTO fromLoanReport(co.com.pragma.model.report.LoanReport report) {
        return LoanReportDTO.builder()
                .totalApprovedLoans(report.getTotalApprovedLoans())
                .manualApprovedLoans(report.getManualApprovedLoans())
                .automaticApprovedLoans(report.getAutomaticApprovedLoans())
                .totalApprovedAmount(report.getTotalApprovedAmount())
                .manualApprovedAmount(report.getManualApprovedAmount())
                .automaticApprovedAmount(report.getAutomaticApprovedAmount())
                .lastUpdated(report.getLastUpdated())
                .lastLoanId(report.getLastLoanId())
                .status("ACTIVE")
                .build();
    }
}
