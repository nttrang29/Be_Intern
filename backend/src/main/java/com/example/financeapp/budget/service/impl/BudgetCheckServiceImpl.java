package com.example.financeapp.budget.service.impl;

import com.example.financeapp.budget.dto.BudgetWarningResponse;
import com.example.financeapp.budget.entity.Budget;
import com.example.financeapp.budget.repository.BudgetRepository;
import com.example.financeapp.budget.service.BudgetCheckService;
import com.example.financeapp.transaction.entity.Transaction;
import com.example.financeapp.transaction.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Implementation của BudgetCheckService
 */
@Service
public class BudgetCheckServiceImpl implements BudgetCheckService {

    @Autowired
    private BudgetRepository budgetRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;

    @Override
    public Budget checkAndMarkExceededBudget(Transaction transaction) {
        // Chỉ kiểm tra giao dịch chi tiêu
        if (!"Chi tiêu".equals(transaction.getTransactionType().getTypeName())) {
            return null;
        }

        Long userId = transaction.getUser().getUserId();
        Long categoryId = transaction.getCategory().getCategoryId();
        Long walletId = transaction.getWallet().getWalletId();
        LocalDate transactionDate = transaction.getTransactionDate().toLocalDate();

        // Tìm các budget áp dụng cho giao dịch này
        // Budget có thể áp dụng cho:
        // 1. Một ví cụ thể (walletId khớp)
        // 2. Tất cả ví (wallet = null)
        List<Budget> applicableBudgets = budgetRepository.findApplicableBudgets(
                userId,
                categoryId,
                walletId,
                transactionDate
        );

        Budget exceededBudget = null;
        BigDecimal maxExceededAmount = BigDecimal.ZERO;

        // Kiểm tra từng budget
        for (Budget budget : applicableBudgets) {
            // Tính tổng số tiền đã chi (bao gồm giao dịch hiện tại)
            BigDecimal currentSpent = calculateCurrentSpent(budget, transaction);
            
            // Kiểm tra vượt hạn mức
            if (currentSpent.compareTo(budget.getAmountLimit()) > 0) {
                BigDecimal exceededAmount = currentSpent.subtract(budget.getAmountLimit());
                
                // Lưu budget bị vượt nhiều nhất
                if (exceededAmount.compareTo(maxExceededAmount) > 0) {
                    maxExceededAmount = exceededAmount;
                    exceededBudget = budget;
                }
            }
        }

        // Đánh dấu giao dịch nếu vượt hạn mức
        if (exceededBudget != null) {
            transaction.setIsExceededBudget(true);
            transaction.setExceededBudgetAmount(maxExceededAmount);
            transaction.setExceededBudgetId(exceededBudget.getBudgetId());
        } else {
            transaction.setIsExceededBudget(false);
            transaction.setExceededBudgetAmount(BigDecimal.ZERO);
            transaction.setExceededBudgetId(null);
        }

        return exceededBudget;
    }

    @Override
    public BudgetWarningResponse checkBudgetWarning(Transaction transaction) {
        // Chỉ kiểm tra giao dịch chi tiêu
        if (!"Chi tiêu".equals(transaction.getTransactionType().getTypeName())) {
            return BudgetWarningResponse.noWarning();
        }

        Long userId = transaction.getUser().getUserId();
        Long categoryId = transaction.getCategory().getCategoryId();
        Long walletId = transaction.getWallet().getWalletId();
        LocalDate transactionDate = transaction.getTransactionDate().toLocalDate();

        // Tìm các budget áp dụng cho giao dịch này
        List<Budget> applicableBudgets = budgetRepository.findApplicableBudgets(
                userId,
                categoryId,
                walletId,
                transactionDate
        );

        BudgetWarningResponse mostSevereWarning = null;
        BigDecimal maxExceededAmount = BigDecimal.ZERO;
        double maxUsagePercentage = 0.0;

        // Kiểm tra từng budget
        for (Budget budget : applicableBudgets) {
            // Tính tổng số tiền đã chi (bao gồm giao dịch hiện tại)
            BigDecimal currentSpent = calculateCurrentSpent(budget, transaction);
            
            // Tính phần trăm sử dụng
            double usagePercentage = 0.0;
            if (budget.getAmountLimit().compareTo(BigDecimal.ZERO) > 0) {
                usagePercentage = currentSpent
                        .divide(budget.getAmountLimit(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
            }

            // Kiểm tra vượt hạn mức (ưu tiên cao nhất)
            if (currentSpent.compareTo(budget.getAmountLimit()) > 0) {
                BigDecimal exceededAmount = currentSpent.subtract(budget.getAmountLimit());
                
                // Lưu budget bị vượt nhiều nhất
                if (exceededAmount.compareTo(maxExceededAmount) > 0) {
                    maxExceededAmount = exceededAmount;
                    mostSevereWarning = BudgetWarningResponse.createExceededWarning(budget, currentSpent);
                }
            } 
            // Kiểm tra gần hết (>= 80% nhưng chưa vượt)
            else if (usagePercentage >= 80.0) {
                // Chỉ lưu nếu chưa có cảnh báo vượt hạn mức và phần trăm cao hơn
                if (mostSevereWarning == null || 
                    (mostSevereWarning.getWarningType().equals("NEARLY_EXHAUSTED") && 
                     usagePercentage > maxUsagePercentage)) {
                    maxUsagePercentage = usagePercentage;
                    mostSevereWarning = BudgetWarningResponse.createNearlyExhaustedWarning(budget, currentSpent);
                }
            }
        }

        return mostSevereWarning != null ? mostSevereWarning : BudgetWarningResponse.noWarning();
    }

    @Override
    public BigDecimal calculateExceededAmount(Budget budget, BigDecimal currentSpent) {
        if (currentSpent.compareTo(budget.getAmountLimit()) > 0) {
            return currentSpent.subtract(budget.getAmountLimit());
        }
        return BigDecimal.ZERO;
    }

    /**
     * Tính tổng số tiền đã chi trong budget (bao gồm giao dịch hiện tại)
     * Lưu ý: Giao dịch hiện tại chưa được lưu vào DB, nên cần cộng thêm
     */
    private BigDecimal calculateCurrentSpent(Budget budget, Transaction currentTransaction) {
        Long walletId = budget.getWallet() != null ? budget.getWallet().getWalletId() : null;
        
        // Tính tổng số tiền đã chi từ DB (chưa bao gồm giao dịch hiện tại)
        BigDecimal spentBefore = transactionRepository.calculateTotalSpent(
                budget.getUser().getUserId(),
                budget.getCategory().getCategoryId(),
                walletId,
                budget.getStartDate(),
                budget.getEndDate()
        );

        // Kiểm tra xem giao dịch hiện tại có thuộc budget này không
        boolean isInBudget = isTransactionInBudget(budget, currentTransaction);
        
        if (isInBudget) {
            // Cộng thêm số tiền của giao dịch hiện tại (vì chưa được lưu vào DB)
            return spentBefore.add(currentTransaction.getAmount());
        }
        
        return spentBefore;
    }

    /**
     * Kiểm tra xem giao dịch có thuộc budget không
     */
    private boolean isTransactionInBudget(Budget budget, Transaction transaction) {
        // Kiểm tra category
        if (!budget.getCategory().getCategoryId().equals(transaction.getCategory().getCategoryId())) {
            return false;
        }

        // Kiểm tra wallet
        if (budget.getWallet() != null) {
            // Budget áp dụng cho một ví cụ thể
            if (!budget.getWallet().getWalletId().equals(transaction.getWallet().getWalletId())) {
                return false;
            }
        }
        // Nếu budget.wallet = null, áp dụng cho tất cả ví

        // Kiểm tra thời gian
        LocalDate transactionDate = transaction.getTransactionDate().toLocalDate();
        if (transactionDate.isBefore(budget.getStartDate()) || transactionDate.isAfter(budget.getEndDate())) {
            return false;
        }

        return true;
    }
}

