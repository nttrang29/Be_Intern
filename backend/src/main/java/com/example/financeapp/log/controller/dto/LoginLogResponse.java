package com.example.financeapp.log.controller.dto;

import com.example.financeapp.log.entity.LoginLog;

import java.time.Instant;

public class LoginLogResponse {

    private Long id;
    private String ipAddress;
    private String userAgent;
    private Instant loginTime;

    public static LoginLogResponse fromEntity(LoginLog entity) {
        LoginLogResponse dto = new LoginLogResponse();
        dto.setId(entity.getId());
        dto.setIpAddress(entity.getIpAddress());
        dto.setUserAgent(entity.getUserAgent());
        dto.setLoginTime(entity.getLoginTime());
        return dto;
    }

    // Getters and Setters
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

    public Instant getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(Instant loginTime) {
        this.loginTime = loginTime;
    }
}

