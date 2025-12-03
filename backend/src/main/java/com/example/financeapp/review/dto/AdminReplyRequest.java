package com.example.financeapp.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO cho admin phản hồi đánh giá
 */
public class AdminReplyRequest {

    @NotBlank(message = "Phản hồi không được để trống")
    @Size(max = 5000, message = "Phản hồi không quá 5000 ký tự")
    private String adminReply;

    // Getters & Setters
    public String getAdminReply() {
        return adminReply;
    }

    public void setAdminReply(String adminReply) {
        this.adminReply = adminReply;
    }
}

