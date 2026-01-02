package com.example.financeapp.review.service;

import com.example.financeapp.review.dto.AppReviewResponse;
import com.example.financeapp.review.dto.CreateAppReviewRequest;

import java.util.List;
import java.util.Map;

/**
 * Service để quản lý đánh giá ứng dụng
 */
public interface AppReviewService {

    /**
     * Tạo đánh giá mới từ user
     */
    AppReviewResponse createReview(Long userId, CreateAppReviewRequest request);

    /**
     * Lấy tất cả đánh giá (cho admin)
     */
    List<AppReviewResponse> getAllReviews();

    /**
     * Lấy đánh giá theo trạng thái
     */
    List<AppReviewResponse> getReviewsByStatus(String status);

    /**
     * Lấy đánh giá của user
     */
    AppReviewResponse getUserReview(Long userId);

    /**
     * Lấy chi tiết một đánh giá
     */
    AppReviewResponse getReviewById(Long reviewId);

    /**
     * Admin phản hồi đánh giá
     */
    AppReviewResponse replyToReview(Long reviewId, String adminReply);

    /**
     * Lấy thống kê đánh giá
     */
    Map<String, Object> getReviewStats();

    /**
     * Xóa đánh giá (admin)
     */
    void deleteReview(Long reviewId);
}

