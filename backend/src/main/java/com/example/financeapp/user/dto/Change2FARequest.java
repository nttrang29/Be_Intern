package com.example.financeapp.user.dto;

public class Change2FARequest {
    private String oldCode; // Mã xác thực cũ
    private String newCode; // Mã xác thực mới
    private String confirmCode; // Nhập lại mã xác thực mới

    public String getOldCode() {
        return oldCode;
    }

    public void setOldCode(String oldCode) {
        this.oldCode = oldCode;
    }

    public String getNewCode() {
        return newCode;
    }

    public void setNewCode(String newCode) {
        this.newCode = newCode;
    }

    public String getConfirmCode() {
        return confirmCode;
    }

    public void setConfirmCode(String confirmCode) {
        this.confirmCode = confirmCode;
    }
}

