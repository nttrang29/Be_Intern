package com.example.financeapp.controller;

import com.example.financeapp.dto.CreateTransactionRequest;
import com.example.financeapp.entity.Transaction;
import com.example.financeapp.entity.User;
import com.example.financeapp.repository.UserRepository;
import com.example.financeapp.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {

    @Autowired private TransactionService transactionService;
    @Autowired private UserRepository userRepository;

    private Long getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .map(User::getUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
    }

    @PostMapping("/expense")
    public ResponseEntity<Map<String, Object>> addExpense(@Valid @RequestBody CreateTransactionRequest request) {
        Map<String, Object> res = new HashMap<>();
        try {
            Transaction tx = transactionService.createExpense(getCurrentUserId(), request);
            res.put("message", "Thêm chi tiêu thành công");
            res.put("transaction", tx);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    @PostMapping("/income")
    public ResponseEntity<Map<String, Object>> addIncome(@Valid @RequestBody CreateTransactionRequest request) {
        Map<String, Object> res = new HashMap<>();
        try {
            Transaction tx = transactionService.createIncome(getCurrentUserId(), request);
            res.put("message", "Thêm thu nhập thành công");
            res.put("transaction", tx);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }
}