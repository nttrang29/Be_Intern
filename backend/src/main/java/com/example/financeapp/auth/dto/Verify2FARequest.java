package com.example.financeapp.auth.dto;

public class Verify2FARequest {
    private String email;
    private String code; // Mã 2FA 6 số

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}

