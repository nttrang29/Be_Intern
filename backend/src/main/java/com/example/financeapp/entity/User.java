package com.example.financeapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash; // Có thể null cho OAuth2 users

    @Column(name = "provider")
    private String provider; // "local" hoặc "google"

    // --- Thêm mới cho chức năng xác thực email ---
    @Column(name = "enabled")
    private boolean enabled = false; // Mặc định: chưa kích hoạt

    @Column(name = "verification_code")
    private String verificationCode; // Mã xác nhận 6 chữ số

    @Column(name = "code_generated_at")
    private LocalDateTime codeGeneratedAt; // Thời gian tạo mã

    @Column(name = "avatar", columnDefinition = "MEDIUMTEXT")
    private String avatar; // URL hoặc base64 của avatar

    @Column(name = "has_default_password")
    private Boolean hasDefaultPassword = false; // true nếu đang dùng mật khẩu mặc định

    // --- Getters & Setters ---

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public LocalDateTime getCodeGeneratedAt() {
        return codeGeneratedAt;
    }

    public void setCodeGeneratedAt(LocalDateTime codeGeneratedAt) {
        this.codeGeneratedAt = codeGeneratedAt;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public boolean isHasDefaultPassword() {
        return hasDefaultPassword;
    }

    public void setHasDefaultPassword(boolean hasDefaultPassword) {
        this.hasDefaultPassword = hasDefaultPassword;
    }
}
