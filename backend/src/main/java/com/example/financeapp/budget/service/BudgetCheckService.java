package com.example.financeapp.budget.service;

import com.example.financeapp.budget.dto.BudgetWarningResponse;
import com.example.financeapp.budget.entity.Budget;
import com.example.financeapp.transaction.entity.Transaction;

import java.math.BigDecimal;

/**
 * Service để kiểm tra và đánh dấu giao dịch vượt hạn mức ngân sách
 */
public interface BudgetCheckService {
    
    /**
     * Kiểm tra và đánh dấu giao dịch nếu vượt hạn mức ngân sách
     * @param transaction Giao dịch cần kiểm tra
     * @return Budget bị vượt (nếu có), null nếu không vượt
     */
    Budget checkAndMarkExceededBudget(Transaction transaction);
    
    /**
     * Kiểm tra và trả về cảnh báo ngân sách (gần hết hoặc vượt hạn mức)
     * @param transaction Giao dịch cần kiểm tra
     * @return BudgetWarningResponse chứa thông tin cảnh báo
     */
    BudgetWarningResponse checkBudgetWarning(Transaction transaction);
    
    /**
     * Tính số tiền vượt hạn mức (nếu có)
     * @param budget Ngân sách
     * @param currentSpent Số tiền đã chi hiện tại (bao gồm giao dịch mới)
     * @return Số tiền vượt hạn mức (0 nếu không vượt)
     */
    BigDecimal calculateExceededAmount(Budget budget, BigDecimal currentSpent);
}

