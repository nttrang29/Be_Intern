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
     * Xóa scheduled transaction
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
}

