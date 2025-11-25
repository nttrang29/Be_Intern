package com.example.financeapp.budget.controller;

import com.example.financeapp.budget.dto.BudgetResponse;
import com.example.financeapp.budget.dto.CreateBudgetRequest;
import com.example.financeapp.budget.entity.Budget;
import com.example.financeapp.transaction.entity.Transaction;
import com.example.financeapp.user.entity.User;
import com.example.financeapp.security.CustomUserDetails;
import com.example.financeapp.budget.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/budgets")
@CrossOrigin(origins = "*")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    /**
     * Tạo ngân sách mới
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createBudget(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateBudgetRequest request
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            Budget budget = budgetService.createBudget(user.getUserId(), request);

            res.put("message", "Tạo hạn mức chi tiêu thành công");
            res.put("budget", budget);
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
     * Lấy danh sách tất cả ngân sách của user
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllBudgets(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            List<BudgetResponse> budgets = budgetService.getAllBudgets(user.getUserId());

            res.put("budgets", budgets);
            res.put("total", budgets.size());
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
     * Lấy chi tiết một ngân sách với thông tin đã chi và còn lại
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getBudgetById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long budgetId
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            BudgetResponse budget = budgetService.getBudgetById(user.getUserId(), budgetId);

            res.put("budget", budget);
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
     * Lấy danh sách giao dịch thuộc một ngân sách cụ thể
     */
    @GetMapping("/{id}/transactions")
    public ResponseEntity<Map<String, Object>> getBudgetTransactions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long budgetId
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            List<Transaction> transactions = budgetService.getBudgetTransactions(user.getUserId(), budgetId);

            res.put("transactions", transactions);
            res.put("total", transactions.size());
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
