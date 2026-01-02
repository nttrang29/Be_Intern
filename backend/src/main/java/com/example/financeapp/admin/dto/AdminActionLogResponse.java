package com.example.financeapp.admin.dto;

import java.time.LocalDateTime;

public class AdminActionLogResponse {
    private Long id;
    private Long adminId;
    private String adminEmail;
    private Long targetUserId;
    private String action;
    private String detail;
    private LocalDateTime createdAt;

    // Builder pattern
    public static AdminActionLogResponseBuilder builder() {
        return new AdminActionLogResponseBuilder();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }
    public String getAdminEmail() { return adminEmail; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }
    public Long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static class AdminActionLogResponseBuilder {
        private Long id;
        private Long adminId;
        private String adminEmail;
        private Long targetUserId;
        private String action;
        private String detail;
        private LocalDateTime createdAt;

        public AdminActionLogResponseBuilder id(Long id) { this.id = id; return this; }
        public AdminActionLogResponseBuilder adminId(Long adminId) { this.adminId = adminId; return this; }
        public AdminActionLogResponseBuilder adminEmail(String adminEmail) { this.adminEmail = adminEmail; return this; }
        public AdminActionLogResponseBuilder targetUserId(Long targetUserId) { this.targetUserId = targetUserId; return this; }
        public AdminActionLogResponseBuilder action(String action) { this.action = action; return this; }
        public AdminActionLogResponseBuilder detail(String detail) { this.detail = detail; return this; }
        public AdminActionLogResponseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public AdminActionLogResponse build() {
            AdminActionLogResponse response = new AdminActionLogResponse();
            response.setId(id);
            response.setAdminId(adminId);
            response.setAdminEmail(adminEmail);
            response.setTargetUserId(targetUserId);
            response.setAction(action);
            response.setDetail(detail);
            response.setCreatedAt(createdAt);
            return response;
        }
    }
}

