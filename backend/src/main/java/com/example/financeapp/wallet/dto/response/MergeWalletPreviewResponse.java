package com.example.financeapp.wallet.dto.response;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO cho preview trước khi gộp ví
 */
public class MergeWalletPreviewResponse {

    // Source wallet info
    private Long sourceWalletId;
    private String sourceWalletName;
    private String sourceCurrency;
    private BigDecimal sourceBalance;
    private int sourceTransactionCount;
    private boolean sourceIsDefault;

    // Target wallet info
    private Long targetWalletId;
    private String targetWalletName;
    private String targetCurrency;
    private BigDecimal targetBalance;
    private int targetTransactionCount;

    // Preview result
    private String finalWalletName;
    private String finalCurrency;
    private BigDecimal finalBalance;
    private int totalTransactions;
    private boolean willTransferDefaultFlag;

    // Validation
    private boolean canProceed;
    private List<String> warnings;

    // Constructors
    public MergeWalletPreviewResponse() {
        this.warnings = new ArrayList<>();
    }

    // Getters & Setters
    public Long getSourceWalletId() {
        return sourceWalletId;
    }

    public void setSourceWalletId(Long sourceWalletId) {
        this.sourceWalletId = sourceWalletId;
    }

    public String getSourceWalletName() {
        return sourceWalletName;
    }

    public void setSourceWalletName(String sourceWalletName) {
        this.sourceWalletName = sourceWalletName;
    }

    public String getSourceCurrency() {
        return sourceCurrency;
    }

    public void setSourceCurrency(String sourceCurrency) {
        this.sourceCurrency = sourceCurrency;
    }

    public BigDecimal getSourceBalance() {
        return sourceBalance;
    }

    public void setSourceBalance(BigDecimal sourceBalance) {
        this.sourceBalance = sourceBalance;
    }

    public int getSourceTransactionCount() {
        return sourceTransactionCount;
    }

    public void setSourceTransactionCount(int sourceTransactionCount) {
        this.sourceTransactionCount = sourceTransactionCount;
    }

    public boolean isSourceIsDefault() {
        return sourceIsDefault;
    }

    public void setSourceIsDefault(boolean sourceIsDefault) {
        this.sourceIsDefault = sourceIsDefault;
    }

    public Long getTargetWalletId() {
        return targetWalletId;
    }

    public void setTargetWalletId(Long targetWalletId) {
        this.targetWalletId = targetWalletId;
    }

    public String getTargetWalletName() {
        return targetWalletName;
    }

    public void setTargetWalletName(String targetWalletName) {
        this.targetWalletName = targetWalletName;
    }

    public String getTargetCurrency() {
        return targetCurrency;
    }

    public void setTargetCurrency(String targetCurrency) {
        this.targetCurrency = targetCurrency;
    }

    public BigDecimal getTargetBalance() {
        return targetBalance;
    }

    public void setTargetBalance(BigDecimal targetBalance) {
        this.targetBalance = targetBalance;
    }

    public int getTargetTransactionCount() {
        return targetTransactionCount;
    }

    public void setTargetTransactionCount(int targetTransactionCount) {
        this.targetTransactionCount = targetTransactionCount;
    }

    public String getFinalWalletName() {
        return finalWalletName;
    }

    public void setFinalWalletName(String finalWalletName) {
        this.finalWalletName = finalWalletName;
    }

    public String getFinalCurrency() {
        return finalCurrency;
    }

    public void setFinalCurrency(String finalCurrency) {
        this.finalCurrency = finalCurrency;
    }

    public BigDecimal getFinalBalance() {
        return finalBalance;
    }

    public void setFinalBalance(BigDecimal finalBalance) {
        this.finalBalance = finalBalance;
    }

    public int getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(int totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public boolean isWillTransferDefaultFlag() {
        return willTransferDefaultFlag;
    }

    public void setWillTransferDefaultFlag(boolean willTransferDefaultFlag) {
        this.willTransferDefaultFlag = willTransferDefaultFlag;
    }

    public boolean isCanProceed() {
        return canProceed;
    }

    public void setCanProceed(boolean canProceed) {
        this.canProceed = canProceed;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
}

