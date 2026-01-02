package com.example.financeapp.admin.dto;

import com.example.financeapp.security.Role;
import com.example.financeapp.user.entity.User;
import java.time.LocalDateTime;

public class AdminUserDetailResponse {

    private Long id;
    private String email;
    private String fullName;
    private Role role;
    private boolean locked;
    private boolean googleAccount;
    private boolean firstLogin;
    private LocalDateTime createdAt;

    public static AdminUserDetailResponse fromEntity(User user) {
        AdminUserDetailResponse dto = new AdminUserDetailResponse();
        dto.setId(user.getUserId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setRole(user.getRole());
        dto.setLocked(user.isLocked());
        dto.setGoogleAccount(user.isGoogleAccount());
        dto.setFirstLogin(user.isFirstLogin());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public boolean isGoogleAccount() { return googleAccount; }
    public void setGoogleAccount(boolean googleAccount) { this.googleAccount = googleAccount; }
    public boolean isFirstLogin() { return firstLogin; }
    public void setFirstLogin(boolean firstLogin) { this.firstLogin = firstLogin; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

