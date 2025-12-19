package com.example.financeapp.wallet.dto.request;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class UpdateTransferRequest {

    @Size(max = 500, message = "Ghi chú không quá 500 ký tự")
    private String note;

    private BigDecimal amount;

    private LocalDateTime transferDate;

    // Getters & Setters
    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getTransferDate() {
        return transferDate;
    }

    public void setTransferDate(LocalDateTime transferDate) {
        this.transferDate = transferDate;
    }
}

