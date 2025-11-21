package com.example.financeapp.wallet.dto.request;

import jakarta.validation.constraints.Size;

public class UpdateTransferRequest {

    @Size(max = 500, message = "Ghi chú không quá 500 ký tự")
    private String note;

    // Getters & Setters
    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}

