package com.example.financeapp.service;

import com.example.financeapp.dto.CreateBudgetRequest;
import com.example.financeapp.entity.Budget;

public interface BudgetService {
    Budget createBudget(Long userId, CreateBudgetRequest request);
}