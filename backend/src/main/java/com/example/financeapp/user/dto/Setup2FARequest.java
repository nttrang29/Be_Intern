package com.example.financeapp.user.dto;

public class Setup2FARequest {
    private String code; // Mã 2FA 6 số để xác nhận setup

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}

