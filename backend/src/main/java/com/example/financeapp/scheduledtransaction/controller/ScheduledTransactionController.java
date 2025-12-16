package com.example.financeapp.scheduledtransaction.controller;

import com.example.financeapp.scheduledtransaction.dto.CreateScheduledTransactionRequest;
import com.example.financeapp.scheduledtransaction.dto.ScheduledTransactionResponse;
import com.example.financeapp.scheduledtransaction.service.ScheduledTransactionService;
import com.example.financeapp.security.CustomUserDetails;
import com.example.financeapp.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/scheduled-transactions")
@CrossOrigin(origins = "*")
public class ScheduledTransactionController {

    @Autowired
    private ScheduledTransactionService scheduledTransactionService;

    /**
     * Preview ngày thực hiện tiếp theo (cho frontend hiển thị mini preview)
     */
    @PostMapping("/preview")
    public ResponseEntity<Map<String, Object>> previewScheduledTransaction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateScheduledTransactionRequest request
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            LocalDate nextExecutionDate = scheduledTransactionService.previewNextExecutionDate(request);
            
            if (nextExecutionDate != null) {
                res.put("hasPreview", true);
                res.put("nextExecutionDate", nextExecutionDate);
                res.put("executionTime", request.getExecutionTime());
                res.put("message", String.format("Lần thực hiện tiếp theo: %s lúc %s", 
                        nextExecutionDate, request.getExecutionTime()));
            } else {
                res.put("hasPreview", false);
                res.put("message", "Chưa chọn thời điểm chạy.");
            }
            
            return ResponseEntity.ok(res);
        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Tạo scheduled transaction mới
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createScheduledTransaction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateScheduledTransactionRequest request
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            ScheduledTransactionResponse scheduled = scheduledTransactionService
                    .createScheduledTransaction(user.getUserId(), request);
            
            res.put("message", "Tạo lịch giao dịch thành công");
            res.put("scheduledTransaction", scheduled);
            return ResponseEntity.ok(res);
            
        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Lấy danh sách tất cả scheduled transactions của user
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllScheduledTransactions(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            List<ScheduledTransactionResponse> scheduledTransactions = 
                    scheduledTransactionService.getAllScheduledTransactions(user.getUserId());
            
            res.put("scheduledTransactions", scheduledTransactions);
            res.put("total", scheduledTransactions.size());
            return ResponseEntity.ok(res);
            
        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Lấy chi tiết một scheduled transaction
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getScheduledTransactionById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long scheduleId
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            ScheduledTransactionResponse scheduled = scheduledTransactionService
                    .getScheduledTransactionById(user.getUserId(), scheduleId);
            
            res.put("scheduledTransaction", scheduled);
            return ResponseEntity.ok(res);
            
        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Hủy scheduled transaction (đổi status thành CANCELLED, không xóa)
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelScheduledTransaction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long scheduleId
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            ScheduledTransactionResponse scheduled = scheduledTransactionService
                    .cancelScheduledTransaction(user.getUserId(), scheduleId);
            
            res.put("message", "Đã hủy lịch giao dịch");
            res.put("scheduledTransaction", scheduled);
            return ResponseEntity.ok(res);
            
        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Xóa scheduled transaction (xóa hoàn toàn khỏi database)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteScheduledTransaction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long scheduleId
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            scheduledTransactionService.deleteScheduledTransaction(user.getUserId(), scheduleId);
            
            res.put("message", "Xóa lịch giao dịch thành công");
            return ResponseEntity.ok(res);
            
        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Lấy lịch sử thực hiện của scheduled transaction
     */
    @GetMapping("/{id}/logs")
    public ResponseEntity<Map<String, Object>> getExecutionLogs(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long scheduleId
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            var logs = scheduledTransactionService.getExecutionLogs(user.getUserId(), scheduleId);

            res.put("logs", logs);
            res.put("total", logs.size());
            return ResponseEntity.ok(res);

        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }
}

