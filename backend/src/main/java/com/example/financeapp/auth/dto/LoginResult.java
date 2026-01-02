package com.example.financeapp.auth.dto;

public class LoginResult {
    private Long userId;
    private String token;
    private boolean requires2FA; // Có cần xác thực 2FA không

    public LoginResult(Long userId, String token) {
        this.userId = userId;
        this.token = token;
        this.requires2FA = false;
    }

    public LoginResult(Long userId, String token, boolean requires2FA) {
        this.userId = userId;
        this.token = token;
        this.requires2FA = requires2FA;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isRequires2FA() {
        return requires2FA;
    }

    public void setRequires2FA(boolean requires2FA) {
        this.requires2FA = requires2FA;
    }
}

