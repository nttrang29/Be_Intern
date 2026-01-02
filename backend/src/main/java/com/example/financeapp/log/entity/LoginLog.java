package com.example.financeapp.log.entity;

import com.example.financeapp.log.model.LoginLogStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "login_logs",
    indexes = {
        @Index(name = "idx_login_logs_user_time", columnList = "user_id, login_time")
    }
)
public class LoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID của user đăng nhập
     */
    @Column(name = "user_id")
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
     * Chuỗi ngắn gọn mô tả thiết bị (ví dụ: "Chrome • Windows")
     */
    @Column(name = "device_summary")
    private String deviceSummary;

    @Column(name = "browser")
    private String browser;

    @Column(name = "operating_system")
    private String operatingSystem;

    @Column(name = "location")
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private LoginLogStatus status;

    @Column(name = "suspicious")
    private boolean suspicious;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "failure_reason")
    private String failureReason;

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

    public Instant getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(Instant loginTime) {
        this.loginTime = loginTime;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getDeviceSummary() {
        return deviceSummary;
    }

    public void setDeviceSummary(String deviceSummary) {
        this.deviceSummary = deviceSummary;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LoginLogStatus getStatus() {
        return status;
    }

    public void setStatus(LoginLogStatus status) {
        this.status = status;
    }

    public boolean isSuspicious() {
        return suspicious;
    }

    public void setSuspicious(boolean suspicious) {
        this.suspicious = suspicious;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}

