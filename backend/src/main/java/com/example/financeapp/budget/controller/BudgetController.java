package com.example.financeapp.controller;

import com.example.financeapp.dto.CreateBudgetRequest;
import com.example.financeapp.entity.Budget;
import com.example.financeapp.entity.User;
import com.example.financeapp.security.CustomUserDetails;
import com.example.financeapp.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/budgets")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;
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
}
