package com.example.financeapp.log.controller.dto;

import com.example.financeapp.log.entity.LoginLog;
import com.example.financeapp.log.model.LoginLogStatus;

import java.time.Instant;

public class LoginLogResponse {

    private Long id;
    private String ipAddress;
    private String userAgent;
    private String device;
    private String browser;
    private String operatingSystem;
    private String location;
    private LoginLogStatus status;
    private boolean suspicious;
    private String failureReason;
    private Instant loginTime;

    public static LoginLogResponse fromEntity(LoginLog entity) {
        LoginLogResponse dto = new LoginLogResponse();
        dto.setId(entity.getId());
        dto.setIpAddress(entity.getIpAddress());
        dto.setUserAgent(entity.getUserAgent());
        dto.setDevice(entity.getDeviceSummary());
        dto.setBrowser(entity.getBrowser());
        dto.setOperatingSystem(entity.getOperatingSystem());
        dto.setLocation(entity.getLocation());
        dto.setStatus(entity.getStatus());
        dto.setSuspicious(entity.isSuspicious());
        dto.setFailureReason(entity.getFailureReason());
        dto.setLoginTime(entity.getLoginTime());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
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

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Instant getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(Instant loginTime) {
        this.loginTime = loginTime;
    }
}

