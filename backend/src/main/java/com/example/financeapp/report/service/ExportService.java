package com.example.financeapp.report.service;

import com.example.financeapp.report.dto.ExportRequest;
import org.springframework.core.io.Resource;

/**
 * Service để export báo cáo ra Excel/PDF
 */
public interface ExportService {
    
    /**
     * Export báo cáo theo yêu cầu
     */
    Resource exportReport(Long userId, ExportRequest request);
    
    /**
     * Export báo cáo giao dịch
     */
    Resource exportTransactions(Long userId, ExportRequest request);
    
    /**
     * Export báo cáo ngân sách
     */
    Resource exportBudgets(Long userId, ExportRequest request);
    
    /**
     * Export báo cáo tổng hợp
     */
    Resource exportSummary(Long userId, ExportRequest request);
}

