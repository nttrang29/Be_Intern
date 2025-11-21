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
    private String passwordHash; // null nếu là OAuth2 user

    @Column(name = "provider")
    private String provider; // local / google / facebook

    // ===== Xác minh email =====
    @Column(name = "enabled")
    private boolean enabled = false;

    @Column(name = "verification_code")
    private String verificationCode;   // Mã OTP hoặc mã xác minh email

    @Column(name = "code_generated_at")
    private LocalDateTime codeGeneratedAt;

    // ===== Giới hạn OTP =====
    @Column(name = "otp_request_count")
    private int otpRequestCount = 0;   // số lần gửi OTP trong 1 giờ

    @Column(name = "otp_last_request")
    private LocalDateTime otpLastRequest;  // lần cuối gửi OTP

    // ===== Avatar người dùng =====
    @Column(name = "avatar", columnDefinition = "MEDIUMTEXT")
    private String avatar;

    // Dùng để đánh dấu tài khoản có đang dùng mật khẩu mặc định không
    @Column(name = "has_default_password")
    private Boolean hasDefaultPassword = false;

    // ===========================
    // GETTERS & SETTERS
    // ===========================
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

    public int getOtpRequestCount() {
        return otpRequestCount;
    }

    public void setOtpRequestCount(int otpRequestCount) {
        this.otpRequestCount = otpRequestCount;
    }

    public LocalDateTime getOtpLastRequest() {
        return otpLastRequest;
    }

    public void setOtpLastRequest(LocalDateTime otpLastRequest) {
        this.otpLastRequest = otpLastRequest;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Boolean getHasDefaultPassword() {
        return hasDefaultPassword;
    }

    public void setHasDefaultPassword(Boolean hasDefaultPassword) {
        this.hasDefaultPassword = hasDefaultPassword;
    }
}
