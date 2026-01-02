package com.example.financeapp.review.dto;

import jakarta.validation.constraints.*;

/**
 * DTO cho request tạo đánh giá ứng dụng
 */
public class CreateAppReviewRequest {

    @Size(max = 100, message = "Tên hiển thị không quá 100 ký tự")
    private String displayName; // Tùy chọn, mặc định "Người dùng ẩn danh"

    @NotNull(message = "Vui lòng chọn mức độ hài lòng")
    @Min(value = 1, message = "Đánh giá tối thiểu 1 sao")
    @Max(value = 5, message = "Đánh giá tối đa 5 sao")
    private Integer rating;

    @NotBlank(message = "Nội dung đánh giá không được để trống")
    @Size(max = 5000, message = "Nội dung đánh giá không quá 5000 ký tự")
    private String content;

    // Getters & Setters
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

