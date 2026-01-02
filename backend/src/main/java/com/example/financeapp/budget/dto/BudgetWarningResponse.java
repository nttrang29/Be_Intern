package com.example.financeapp.budget.dto;

import com.example.financeapp.budget.entity.Budget;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DTO để trả về thông tin cảnh báo ngân sách
 */
public class BudgetWarningResponse {
    private boolean hasWarning; // Có cảnh báo không
    private String warningType; // "NEARLY_EXHAUSTED" (>= 80%) hoặc "EXCEEDED" (vượt hạn mức)
    private Long budgetId;
    private String budgetName; // Tên ngân sách (category name)
    private BigDecimal amountLimit;
    private BigDecimal currentSpent;
    private BigDecimal remainingAmount;
    private BigDecimal exceededAmount; // Số tiền vượt (0 nếu không vượt)
    private Double usagePercentage;
    private String message; // Thông báo cho người dùng
    
    // Thêm các field để hiển thị chi tiết trong modal preview
    private BigDecimal spentBeforeTransaction; // Đã chi TRƯỚC giao dịch này
    private BigDecimal remainingBeforeTransaction; // Còn lại TRƯỚC giao dịch này
    private BigDecimal transactionAmount; // Số tiền giao dịch này
    private BigDecimal totalAfterTransaction; // Tổng SAU giao dịch này
    private BigDecimal remainingAfterTransaction; // Còn lại SAU giao dịch này
    private Double usagePercentageAfterTransaction; // % sử dụng SAU giao dịch này

    public BudgetWarningResponse() {}

    /**
     * Tạo cảnh báo khi gần hết ngân sách (>= 80% nhưng chưa vượt)
     */
    public static BudgetWarningResponse createNearlyExhaustedWarning(
            Budget budget,
            BigDecimal currentSpent) {
        BudgetWarningResponse response = new BudgetWarningResponse();
        response.setHasWarning(true);
        response.setWarningType("NEARLY_EXHAUSTED");
        response.setBudgetId(budget.getBudgetId());
        response.setBudgetName(budget.getCategory().getCategoryName());
        response.setAmountLimit(budget.getAmountLimit());
        response.setCurrentSpent(currentSpent);

        BigDecimal remaining = budget.getAmountLimit().subtract(currentSpent);
        response.setRemainingAmount(remaining.max(BigDecimal.ZERO));
        response.setExceededAmount(BigDecimal.ZERO);

        // Tính phần trăm
        if (budget.getAmountLimit().compareTo(BigDecimal.ZERO) > 0) {
            double percentage = currentSpent
                    .divide(budget.getAmountLimit(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
            response.setUsagePercentage(percentage);
        } else {
            response.setUsagePercentage(0.0);
        }

        response.setMessage(String.format(
            "⚠️ Ngân sách \"%s\" đã sử dụng %.1f%%. Còn lại: %s VND",
            budget.getCategory().getCategoryName(),
            response.getUsagePercentage(),
            response.getRemainingAmount()
        ));

        return response;
    }

    /**
     * Tạo cảnh báo khi vượt hạn mức
     */
    public static BudgetWarningResponse createExceededWarning(
            Budget budget,
            BigDecimal currentSpent) {
        BudgetWarningResponse response = new BudgetWarningResponse();
        response.setHasWarning(true);
        response.setWarningType("EXCEEDED");
        response.setBudgetId(budget.getBudgetId());
        response.setBudgetName(budget.getCategory().getCategoryName());
        response.setAmountLimit(budget.getAmountLimit());
        response.setCurrentSpent(currentSpent);
        response.setRemainingAmount(BigDecimal.ZERO);

        BigDecimal exceeded = currentSpent.subtract(budget.getAmountLimit());
        response.setExceededAmount(exceeded);

        // Tính phần trăm
        if (budget.getAmountLimit().compareTo(BigDecimal.ZERO) > 0) {
            double percentage = currentSpent
                    .divide(budget.getAmountLimit(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
            response.setUsagePercentage(percentage);
        } else {
            response.setUsagePercentage(0.0);
        }

        response.setMessage(String.format(
            "⚠️ Ngân sách \"%s\" đã vượt hạn mức %s VND",
            budget.getCategory().getCategoryName(),
            response.getExceededAmount()
        ));

        return response;
    }

    /**
     * Tạo response không có cảnh báo
     */
    public static BudgetWarningResponse noWarning() {
        BudgetWarningResponse response = new BudgetWarningResponse();
        response.setHasWarning(false);
        response.setWarningType(null);
        response.setMessage(null);
        return response;
    }

    // Getters & Setters
    public boolean isHasWarning() {
        return hasWarning;
    }

    public void setHasWarning(boolean hasWarning) {
        this.hasWarning = hasWarning;
    }

    public String getWarningType() {
        return warningType;
    }

    public void setWarningType(String warningType) {
        this.warningType = warningType;
    }

    public Long getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(Long budgetId) {
        this.budgetId = budgetId;
    }

    public String getBudgetName() {
        return budgetName;
    }

    public void setBudgetName(String budgetName) {
        this.budgetName = budgetName;
    }

    public BigDecimal getAmountLimit() {
        return amountLimit;
    }

    public void setAmountLimit(BigDecimal amountLimit) {
        this.amountLimit = amountLimit;
    }

    public BigDecimal getCurrentSpent() {
        return currentSpent;
    }

    public void setCurrentSpent(BigDecimal currentSpent) {
        this.currentSpent = currentSpent;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public BigDecimal getExceededAmount() {
        return exceededAmount;
    }

    public void setExceededAmount(BigDecimal exceededAmount) {
        this.exceededAmount = exceededAmount;
    }

    public Double getUsagePercentage() {
        return usagePercentage;
    }

    public void setUsagePercentage(Double usagePercentage) {
        this.usagePercentage = usagePercentage;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // Getters & Setters cho các field preview
    public BigDecimal getSpentBeforeTransaction() {
        return spentBeforeTransaction;
    }

    public void setSpentBeforeTransaction(BigDecimal spentBeforeTransaction) {
        this.spentBeforeTransaction = spentBeforeTransaction;
    }

    public BigDecimal getRemainingBeforeTransaction() {
        return remainingBeforeTransaction;
    }

    public void setRemainingBeforeTransaction(BigDecimal remainingBeforeTransaction) {
        this.remainingBeforeTransaction = remainingBeforeTransaction;
    }

    public BigDecimal getTransactionAmount() {
        return transactionAmount;
    }

    public void setTransactionAmount(BigDecimal transactionAmount) {
        this.transactionAmount = transactionAmount;
    }

    public BigDecimal getTotalAfterTransaction() {
        return totalAfterTransaction;
    }

    public void setTotalAfterTransaction(BigDecimal totalAfterTransaction) {
        this.totalAfterTransaction = totalAfterTransaction;
    }

    public BigDecimal getRemainingAfterTransaction() {
        return remainingAfterTransaction;
    }

    public void setRemainingAfterTransaction(BigDecimal remainingAfterTransaction) {
        this.remainingAfterTransaction = remainingAfterTransaction;
    }

    public Double getUsagePercentageAfterTransaction() {
        return usagePercentageAfterTransaction;
    }

    public void setUsagePercentageAfterTransaction(Double usagePercentageAfterTransaction) {
        this.usagePercentageAfterTransaction = usagePercentageAfterTransaction;
    }
}

