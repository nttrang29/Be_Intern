package com.example.financeapp.user.entity;

import com.example.financeapp.security.Role;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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

    // ===== Avatar người dùng =====
    @Column(name = "avatar", columnDefinition = "MEDIUMTEXT")
    private String avatar;

    // ===== Quyền hệ thống =====
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role = Role.USER;

    @Column(name = "locked")
    private boolean locked = false;

    // ===== Google Login =====
    @Column(name = "google_account")
    private boolean googleAccount = false;

    @Column(name = "first_login")
    private boolean firstLogin = false;

    // ===== Quên mật khẩu =====
    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "reset_token_expired_at")
    private LocalDateTime resetTokenExpiredAt;

    // ===== Soft delete + hoạt động gần nhất =====
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    // ===== Auto Backup =====
    @Column(name = "auto_backup_enabled")
    private Boolean autoBackupEnabled = false; // Mặc định tắt auto backup (Boolean để tránh lỗi null với user cũ)

    // ===== 2FA (Xác thực 2 lớp) =====
    @Column(name = "two_factor_enabled")
    private Boolean twoFactorEnabled = false; // Trạng thái bật/tắt 2FA (Boolean để tránh lỗi null với user cũ)

    @Column(name = "two_factor_secret", length = 255)
    private String twoFactorSecret; // Mã pin 2FA đã được hash (BCrypt hash ~60 ký tự)

    // ===== Auditing =====
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.googleAccount) {
            this.firstLogin = true;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

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

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isGoogleAccount() {
        return googleAccount;
    }

    public void setGoogleAccount(boolean googleAccount) {
        this.googleAccount = googleAccount;
    }

    public boolean isFirstLogin() {
        return firstLogin;
    }

    public void setFirstLogin(boolean firstLogin) {
        this.firstLogin = firstLogin;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public LocalDateTime getResetTokenExpiredAt() {
        return resetTokenExpiredAt;
    }

    public void setResetTokenExpiredAt(LocalDateTime resetTokenExpiredAt) {
        this.resetTokenExpiredAt = resetTokenExpiredAt;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(LocalDateTime lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    public boolean isAutoBackupEnabled() {
        return autoBackupEnabled != null ? autoBackupEnabled : false; // Trả về false nếu null (user cũ)
    }

    public void setAutoBackupEnabled(Boolean autoBackupEnabled) {
        this.autoBackupEnabled = autoBackupEnabled != null ? autoBackupEnabled : false;
    }

    public boolean isTwoFactorEnabled() {
        return twoFactorEnabled != null ? twoFactorEnabled : false; // Trả về false nếu null (user cũ)
    }

    public void setTwoFactorEnabled(Boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled != null ? twoFactorEnabled : false;
    }

    public String getTwoFactorSecret() {
        return twoFactorSecret;
    }

    public void setTwoFactorSecret(String twoFactorSecret) {
        this.twoFactorSecret = twoFactorSecret;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
