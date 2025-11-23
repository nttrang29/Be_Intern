package com.example.financeapp.log.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "login_logs")
public class LoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID của user đăng nhập
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Địa chỉ IP khi đăng nhập
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Thông tin thiết bị / trình duyệt
     */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    /**
     * Thời gian đăng nhập
     */
    @Column(name = "login_time", nullable = false, updatable = false)
    private Instant loginTime;

    @PrePersist
    public void prePersist() {
        if (loginTime == null) {
            loginTime = Instant.now();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Instant getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(Instant loginTime) {
        this.loginTime = loginTime;
    }
}

