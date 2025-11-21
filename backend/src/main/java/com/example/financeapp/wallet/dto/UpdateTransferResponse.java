package com.example.financeapp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class UpdateTransferResponse {
    private Long transferId;
    private String note;
    private LocalDateTime updatedAt;

    // Constructors
    public UpdateTransferResponse() {
    }

    public UpdateTransferResponse(Long transferId, String note, LocalDateTime updatedAt) {
        this.transferId = transferId;
        this.note = note;
        this.updatedAt = updatedAt;
    }

    // Getters & Setters
    public Long getTransferId() {
        return transferId;
    }

    public void setTransferId(Long transferId) {
        this.transferId = transferId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

