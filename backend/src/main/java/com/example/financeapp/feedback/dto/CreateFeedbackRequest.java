package com.example.financeapp.feedback.dto;

import com.example.financeapp.feedback.entity.FeedbackType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO để tạo feedback
 */
public class CreateFeedbackRequest {

    @NotNull(message = "Vui lòng chọn loại phản hồi")
    private FeedbackType type;

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 200, message = "Tiêu đề không quá 200 ký tự")
    private String subject;

    @NotBlank(message = "Nội dung phản hồi không được để trống")
    @Size(max = 5000, message = "Nội dung phản hồi không quá 5000 ký tự")
    private String message;

    @Email(message = "Email không hợp lệ")
    @Size(max = 100, message = "Email không quá 100 ký tự")
    private String contactEmail; // Optional: email để liên hệ lại

    // Getters & Setters
    public FeedbackType getType() { return type; }
    public void setType(FeedbackType type) { this.type = type; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
}

