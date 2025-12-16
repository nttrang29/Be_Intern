package com.example.financeapp.report.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * DTO cho request export báo cáo
 */
public class ExportRequest {
    
    @NotNull(message = "Vui lòng chọn định dạng xuất (EXCEL hoặc PDF)")
    private ExportFormat format;
    
    private LocalDate startDate; // null = không giới hạn
    private LocalDate endDate;   // null = không giới hạn
    private Long walletId;       // null = tất cả ví
    private ReportType reportType = ReportType.TRANSACTIONS; // Mặc định là báo cáo giao dịch
    private String range;        // "day", "week", "month", "year" - để tính toán startDate/endDate tự động

    public enum ExportFormat {
        EXCEL, PDF
    }

    public enum ReportType {
        TRANSACTIONS,  // Báo cáo giao dịch
        BUDGETS,       // Báo cáo ngân sách
        SUMMARY        // Báo cáo tổng hợp
    }

    // Getters & Setters
    public ExportFormat getFormat() { return format; }
    public void setFormat(ExportFormat format) { this.format = format; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Long getWalletId() { return walletId; }
    public void setWalletId(Long walletId) { this.walletId = walletId; }

    public ReportType getReportType() { return reportType; }
    public void setReportType(ReportType reportType) { this.reportType = reportType; }

    public String getRange() { return range; }
    public void setRange(String range) { this.range = range; }
}

