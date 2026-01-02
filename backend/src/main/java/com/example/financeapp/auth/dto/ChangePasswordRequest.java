package com.example.financeapp.auth.dto;

public class ChangePasswordRequest {
    private String oldPassword;  // Nullable cho lần đổi đầu tiên từ mật khẩu mặc định
    private String newPassword;
    private String confirmPassword;

    // Getters & Setters
    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}

