package com.example.financeapp.budget.service;

import com.example.financeapp.budget.dto.BudgetResponse;
import com.example.financeapp.budget.dto.CreateBudgetRequest;
import com.example.financeapp.budget.entity.Budget;
import com.example.financeapp.transaction.entity.Transaction;

import java.util.List;

public interface BudgetService {
    Budget createBudget(Long userId, CreateBudgetRequest request);
    
    /**
     * Lấy danh sách tất cả budgets của user với thông tin đã chi và còn lại
     */
    List<BudgetResponse> getAllBudgets(Long userId);
    
    /**
     * Lấy chi tiết một budget với thông tin đã chi và còn lại
     */
    BudgetResponse getBudgetById(Long userId, Long budgetId);
    
    /**
     * Lấy danh sách giao dịch thuộc một budget cụ thể
     */
    List<Transaction> getBudgetTransactions(Long userId, Long budgetId);
}