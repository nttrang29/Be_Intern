package com.example.financeapp.feedback.dto;

import com.example.financeapp.feedback.entity.Feedback;
import com.example.financeapp.feedback.entity.FeedbackStatus;
import com.example.financeapp.feedback.entity.FeedbackType;
import java.time.LocalDateTime;

/**
 * DTO response cho feedback
 */
public class FeedbackResponse {
    private Long feedbackId;
    private Long userId;
    private String userEmail;
    private String userName;
    private FeedbackType type;
    private FeedbackStatus status;
    private String subject;
    private String message;
    private String contactEmail;
    private String adminResponse;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime resolvedAt;

    public FeedbackResponse() {}

    public static FeedbackResponse fromEntity(Feedback feedback) {
        FeedbackResponse response = new FeedbackResponse();
        response.setFeedbackId(feedback.getFeedbackId());
        response.setUserId(feedback.getUser().getUserId());
        response.setUserEmail(feedback.getUser().getEmail());
        response.setUserName(feedback.getUser().getFullName());
        response.setType(feedback.getType());
        response.setStatus(feedback.getStatus());
        response.setSubject(feedback.getSubject());
        response.setMessage(feedback.getMessage());
        response.setContactEmail(feedback.getContactEmail());
        response.setAdminResponse(feedback.getAdminResponse());
        response.setCreatedAt(feedback.getCreatedAt());
        response.setUpdatedAt(feedback.getUpdatedAt());
        response.setReviewedAt(feedback.getReviewedAt());
        response.setResolvedAt(feedback.getResolvedAt());
        return response;
    }

    // Getters & Setters
    public Long getFeedbackId() { return feedbackId; }
    public void setFeedbackId(Long feedbackId) { this.feedbackId = feedbackId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public FeedbackType getType() { return type; }
    public void setType(FeedbackType type) { this.type = type; }

    public FeedbackStatus getStatus() { return status; }
    public void setStatus(FeedbackStatus status) { this.status = status; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getAdminResponse() { return adminResponse; }
    public void setAdminResponse(String adminResponse) { this.adminResponse = adminResponse; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}

