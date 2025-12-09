package com.example.financeapp.user.dto;

public class Setup2FAResponse {
    private String code; // Mã 2FA 6 số được tạo (gửi qua email)

    public Setup2FAResponse(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}

