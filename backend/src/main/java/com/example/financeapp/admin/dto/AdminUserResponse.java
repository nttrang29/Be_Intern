package com.example.financeapp.admin.dto;

import com.example.financeapp.security.Role;
import java.time.LocalDateTime;

public class AdminUserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String avatar;
    private Role role;
    private boolean locked;
    private boolean googleAccount;
    private boolean firstLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Builder pattern
    public static AdminUserResponseBuilder builder() {
        return new AdminUserResponseBuilder();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
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
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static class AdminUserResponseBuilder {
        private Long id;
        private String fullName;
        private String email;
        private String avatar;
        private Role role;
        private boolean locked;
        private boolean googleAccount;
        private boolean firstLogin;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public AdminUserResponseBuilder id(Long id) { this.id = id; return this; }
        public AdminUserResponseBuilder fullName(String fullName) { this.fullName = fullName; return this; }
        public AdminUserResponseBuilder email(String email) { this.email = email; return this; }
        public AdminUserResponseBuilder avatar(String avatar) { this.avatar = avatar; return this; }
        public AdminUserResponseBuilder role(Role role) { this.role = role; return this; }
        public AdminUserResponseBuilder locked(boolean locked) { this.locked = locked; return this; }
        public AdminUserResponseBuilder googleAccount(boolean googleAccount) { this.googleAccount = googleAccount; return this; }
        public AdminUserResponseBuilder firstLogin(boolean firstLogin) { this.firstLogin = firstLogin; return this; }
        public AdminUserResponseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public AdminUserResponseBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public AdminUserResponse build() {
            AdminUserResponse response = new AdminUserResponse();
            response.setId(id);
            response.setFullName(fullName);
            response.setEmail(email);
            response.setAvatar(avatar);
            response.setRole(role);
            response.setLocked(locked);
            response.setGoogleAccount(googleAccount);
            response.setFirstLogin(firstLogin);
            response.setCreatedAt(createdAt);
            response.setUpdatedAt(updatedAt);
            return response;
        }
    }
}

