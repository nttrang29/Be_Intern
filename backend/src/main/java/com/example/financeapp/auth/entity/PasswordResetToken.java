package com.example.financeapp.auth.entity;

import com.example.financeapp.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String token;  // UUID dùng để reset password

    private String otp; // OTP để xác thực bước 2

    @Column(name = "otp_expired_at")
    private LocalDateTime otpExpiredAt;

    @Column(name = "token_expired_at")
    private LocalDateTime tokenExpiredAt;

    @Enumerated(EnumType.STRING)
    private Status status; // PENDING, VERIFIED, CONSUMED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public enum Status {
        PENDING,     // Đã gửi OTP nhưng chưa xác thực
        VERIFIED,    // OTP đã verify — chờ reset password
        CONSUMED     // Token đã dùng, không dùng lại được
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public LocalDateTime getOtpExpiredAt() {
        return otpExpiredAt;
    }

    public void setOtpExpiredAt(LocalDateTime otpExpiredAt) {
        this.otpExpiredAt = otpExpiredAt;
    }

    public LocalDateTime getTokenExpiredAt() {
        return tokenExpiredAt;
    }

    public void setTokenExpiredAt(LocalDateTime tokenExpiredAt) {
        this.tokenExpiredAt = tokenExpiredAt;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}

