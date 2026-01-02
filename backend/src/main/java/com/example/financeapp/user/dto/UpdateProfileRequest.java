package com.example.financeapp.user.dto;

public class UpdateProfileRequest {
    private String fullName;
    private String avatar; // Base64 string hoặc URL

    // Getters & Setters
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}

