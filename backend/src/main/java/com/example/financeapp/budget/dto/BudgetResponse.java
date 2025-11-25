package com.example.financeapp.budget.dto;

import com.example.financeapp.budget.entity.Budget;
import com.example.financeapp.category.entity.Category;
import com.example.financeapp.wallet.entity.Wallet;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO response cho Budget với thông tin đã chi và còn lại
 */
public class BudgetResponse {
    private Long budgetId;
    private Long categoryId;
    private String categoryName;
    private Long walletId;
    private String walletName;
    private BigDecimal amountLimit;
    private BigDecimal spentAmount;
    private BigDecimal remainingAmount;
    private BigDecimal exceededAmount; // Số tiền vượt hạn mức (0 nếu không vượt)
    private Double usagePercentage;
    private String status; // "OK", "WARNING", "EXCEEDED"
    private String budgetStatus; // "ACTIVE", "COMPLETED" - trạng thái ngân sách theo thời gian
    private LocalDate startDate;
    private LocalDate endDate;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor
    public BudgetResponse() {}

    // Factory method từ Budget entity + spentAmount
    public static BudgetResponse fromBudget(Budget budget, BigDecimal spentAmount) {
        BudgetResponse response = new BudgetResponse();
        response.setBudgetId(budget.getBudgetId());
        
        Category category = budget.getCategory();
        response.setCategoryId(category.getCategoryId());
        response.setCategoryName(category.getCategoryName());
        
        Wallet wallet = budget.getWallet();
        if (wallet != null) {
            response.setWalletId(wallet.getWalletId());
            response.setWalletName(wallet.getWalletName());
        } else {
            response.setWalletId(null);
            response.setWalletName("Tất cả ví");
        }
        
        response.setAmountLimit(budget.getAmountLimit());
        response.setSpentAmount(spentAmount);
        
        // Tính số tiền còn lại
        BigDecimal remaining = budget.getAmountLimit().subtract(spentAmount);
        response.setRemainingAmount(remaining.max(BigDecimal.ZERO)); // Không cho âm
        
        // Tính số tiền vượt hạn mức
        BigDecimal exceeded = spentAmount.subtract(budget.getAmountLimit());
        response.setExceededAmount(exceeded.max(BigDecimal.ZERO)); // 0 nếu không vượt
        
        // Tính phần trăm sử dụng
        if (budget.getAmountLimit().compareTo(BigDecimal.ZERO) > 0) {
            double percentage = spentAmount
                    .divide(budget.getAmountLimit(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
            response.setUsagePercentage(percentage);
        } else {
            response.setUsagePercentage(0.0);
        }
        
        // Xác định trạng thái (OK, WARNING, EXCEEDED)
        if (spentAmount.compareTo(budget.getAmountLimit()) > 0) {
            response.setStatus("EXCEEDED"); // Vượt ngân sách
        } else if (response.getUsagePercentage() >= 80.0) {
            response.setStatus("WARNING"); // Sắp hết (>= 80%)
        } else {
            response.setStatus("OK"); // Bình thường
        }
        
        // Trạng thái ngân sách theo thời gian (ACTIVE, COMPLETED)
        if (budget.getStatus() != null) {
            response.setBudgetStatus(budget.getStatus().name());
        } else {
            // Tự động xác định nếu chưa có status
            LocalDate today = LocalDate.now();
            if (today.isAfter(budget.getEndDate())) {
                response.setBudgetStatus("COMPLETED");
            } else {
                response.setBudgetStatus("ACTIVE");
            }
        }
        
        response.setStartDate(budget.getStartDate());
        response.setEndDate(budget.getEndDate());
        response.setNote(budget.getNote());
        response.setCreatedAt(budget.getCreatedAt());
        response.setUpdatedAt(budget.getUpdatedAt());
        
        return response;
    }

    // Getters & Setters
    public Long getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(Long budgetId) {
        this.budgetId = budgetId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public String getWalletName() {
        return walletName;
    }

    public void setWalletName(String walletName) {
        this.walletName = walletName;
    }

    public BigDecimal getAmountLimit() {
        return amountLimit;
    }

    public void setAmountLimit(BigDecimal amountLimit) {
        this.amountLimit = amountLimit;
    }

    public BigDecimal getSpentAmount() {
        return spentAmount;
    }

    public void setSpentAmount(BigDecimal spentAmount) {
        this.spentAmount = spentAmount;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public Double getUsagePercentage() {
        return usagePercentage;
    }

    public void setUsagePercentage(Double usagePercentage) {
        this.usagePercentage = usagePercentage;
    }

    public BigDecimal getExceededAmount() {
        return exceededAmount;
    }

    public void setExceededAmount(BigDecimal exceededAmount) {
        this.exceededAmount = exceededAmount;
    }

    public String getStatus() {
        return status;
    }
    
    public String getBudgetStatus() {
        return budgetStatus;
    }

    public void setBudgetStatus(String budgetStatus) {
        this.budgetStatus = budgetStatus;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

