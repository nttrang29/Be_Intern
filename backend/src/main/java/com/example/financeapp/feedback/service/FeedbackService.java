package com.example.financeapp.feedback.service;

import com.example.financeapp.feedback.dto.CreateFeedbackRequest;
import com.example.financeapp.feedback.dto.FeedbackResponse;

import java.util.List;

/**
 * Service để quản lý feedback
 */
public interface FeedbackService {
    
    /**
     * Tạo feedback mới từ user
     */
    FeedbackResponse createFeedback(Long userId, CreateFeedbackRequest request);
    
    /**
     * Lấy danh sách feedback của user
     */
    List<FeedbackResponse> getUserFeedbacks(Long userId);
    
    /**
     * Lấy chi tiết một feedback
     */
    FeedbackResponse getFeedbackById(Long userId, Long feedbackId);
}

