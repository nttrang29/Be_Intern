package com.example.financeapp.budget.service;

import com.example.financeapp.budget.dto.BudgetWarningResponse;
import com.example.financeapp.budget.entity.Budget;
import com.example.financeapp.transaction.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

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
     * Preview cảnh báo ngân sách TRƯỚC KHI tạo transaction (cho frontend hiển thị modal)
     * @param userId ID người dùng
     * @param categoryId ID danh mục
     * @param walletId ID ví
     * @param amount Số tiền giao dịch
     * @param transactionDate Ngày giao dịch
     * @return BudgetWarningResponse chứa thông tin cảnh báo chi tiết
     */
    BudgetWarningResponse previewBudgetWarning(
            Long userId,
            Long categoryId,
            Long walletId,
            BigDecimal amount,
            LocalDate transactionDate
    );

    /**
     * Tính số tiền vượt hạn mức (nếu có)
     * @param budget Ngân sách
     * @param currentSpent Số tiền đã chi hiện tại (bao gồm giao dịch mới)
     * @return Số tiền vượt hạn mức (0 nếu không vượt)
     */
    BigDecimal calculateExceededAmount(Budget budget, BigDecimal currentSpent);
}

