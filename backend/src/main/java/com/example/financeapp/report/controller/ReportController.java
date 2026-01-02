package com.example.financeapp.report.controller;

import com.example.financeapp.report.dto.ExportRequest;
import com.example.financeapp.report.service.ExportService;
import com.example.financeapp.security.CustomUserDetails;
import com.example.financeapp.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired
    private ExportService exportService;

    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Export báo cáo theo yêu cầu
     */
    @PostMapping("/export")
    public ResponseEntity<Resource> exportReport(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ExportRequest request
    ) {
        try {
            User user = userDetails.getUser();
            Resource resource = exportService.exportReport(user.getUserId(), request);
            
            // Tạo tên file
            String fileName = generateFileName(request);
            String contentType = request.getFormat() == ExportRequest.ExportFormat.EXCEL
                    ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    : "application/pdf";
            String fileExtension = request.getFormat() == ExportRequest.ExportFormat.EXCEL ? ".xlsx" : ".pdf";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + fileExtension + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
                    
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xuất báo cáo: " + e.getMessage(), e);
        }
    }

    /**
     * Export báo cáo giao dịch
     */
    @PostMapping("/export/transactions")
    public ResponseEntity<Resource> exportTransactions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ExportRequest request
    ) {
        try {
            User user = userDetails.getUser();
            request.setReportType(ExportRequest.ReportType.TRANSACTIONS);
            Resource resource = exportService.exportTransactions(user.getUserId(), request);
            
            String fileName = "BaoCaoGiaoDich_" + LocalDateTime.now().format(FILE_DATE_FORMATTER);
            String contentType = request.getFormat() == ExportRequest.ExportFormat.EXCEL
                    ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    : "application/pdf";
            String fileExtension = request.getFormat() == ExportRequest.ExportFormat.EXCEL ? ".xlsx" : ".pdf";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + fileExtension + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
                    
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xuất báo cáo giao dịch: " + e.getMessage(), e);
        }
    }

    /**
     * Export báo cáo ngân sách
     */
    @PostMapping("/export/budgets")
    public ResponseEntity<Resource> exportBudgets(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ExportRequest request
    ) {
        try {
            User user = userDetails.getUser();
            request.setReportType(ExportRequest.ReportType.BUDGETS);
            Resource resource = exportService.exportBudgets(user.getUserId(), request);
            
            String fileName = "BaoCaoNganSach_" + LocalDateTime.now().format(FILE_DATE_FORMATTER);
            String contentType = request.getFormat() == ExportRequest.ExportFormat.EXCEL
                    ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    : "application/pdf";
            String fileExtension = request.getFormat() == ExportRequest.ExportFormat.EXCEL ? ".xlsx" : ".pdf";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + fileExtension + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
                    
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xuất báo cáo ngân sách: " + e.getMessage(), e);
        }
    }

    /**
     * Export báo cáo tổng hợp
     */
    @PostMapping("/export/summary")
    public ResponseEntity<Resource> exportSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ExportRequest request
    ) {
        try {
            User user = userDetails.getUser();
            request.setReportType(ExportRequest.ReportType.SUMMARY);
            Resource resource = exportService.exportSummary(user.getUserId(), request);
            
            String fileName = "BaoCaoTongHop_" + LocalDateTime.now().format(FILE_DATE_FORMATTER);
            String contentType = request.getFormat() == ExportRequest.ExportFormat.EXCEL
                    ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    : "application/pdf";
            String fileExtension = request.getFormat() == ExportRequest.ExportFormat.EXCEL ? ".xlsx" : ".pdf";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + fileExtension + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
                    
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xuất báo cáo tổng hợp: " + e.getMessage(), e);
        }
    }

    /**
     * Export PDF cho ReportsPage (theo wallet và range)
     */
    @PostMapping("/export/wallet-pdf")
    public ResponseEntity<Resource> exportWalletPDF(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ExportRequest request
    ) {
        try {
            User user = userDetails.getUser();
            request.setReportType(ExportRequest.ReportType.TRANSACTIONS);
            request.setFormat(ExportRequest.ExportFormat.PDF);
            
            Resource resource = exportService.exportTransactions(user.getUserId(), request);
            
            // Tạo tên file
            String walletName = "TatCaVi";
            if (request.getWalletId() != null) {
                // Có thể lấy tên ví từ request hoặc từ database
                walletName = "Vi_" + request.getWalletId();
            }
            String rangeStr = request.getRange() != null ? request.getRange() : "all";
            String fileName = String.format("BaoCao_%s_%s_%s", 
                walletName, rangeStr, LocalDateTime.now().format(FILE_DATE_FORMATTER));
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);
                    
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xuất báo cáo PDF: " + e.getMessage(), e);
        }
    }

    private String generateFileName(ExportRequest request) {
        String prefix = switch (request.getReportType()) {
            case TRANSACTIONS -> "BaoCaoGiaoDich";
            case BUDGETS -> "BaoCaoNganSach";
            case SUMMARY -> "BaoCaoTongHop";
        };
        return prefix + "_" + LocalDateTime.now().format(FILE_DATE_FORMATTER);
    }
}

