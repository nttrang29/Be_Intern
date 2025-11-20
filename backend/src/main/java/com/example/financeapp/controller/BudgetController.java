package com.example.financeapp.controller;

import com.example.financeapp.dto.BudgetDTO;
import com.example.financeapp.dto.BudgetRequest;
import com.example.financeapp.service.BudgetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    @PostMapping
    public ResponseEntity<BudgetDTO> create(
            @RequestBody BudgetRequest request,
            Authentication auth
    ) {
        Long userId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(budgetService.createBudget(userId, request));
    }

    @GetMapping
    public ResponseEntity<List<BudgetDTO>> getAll(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(budgetService.getBudgets(userId));
    }
}
