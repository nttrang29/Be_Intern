package com.example.financeapp.feedback.controller;

import com.example.financeapp.feedback.dto.CreateFeedbackRequest;
import com.example.financeapp.feedback.dto.FeedbackResponse;
import com.example.financeapp.feedback.service.FeedbackService;
import com.example.financeapp.security.CustomUserDetails;
import com.example.financeapp.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/feedback")
@CrossOrigin(origins = "*")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    /**
     * Gửi phản hồi/báo lỗi
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createFeedback(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateFeedbackRequest request
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            FeedbackResponse feedback = feedbackService.createFeedback(user.getUserId(), request);
            
            res.put("message", "Cảm ơn bạn đã gửi phản hồi! Chúng tôi sẽ xem xét và phản hồi sớm nhất có thể.");
            res.put("feedback", feedback);
            return ResponseEntity.ok(res);
            
        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Lấy danh sách phản hồi của user
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserFeedbacks(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            List<FeedbackResponse> feedbacks = feedbackService.getUserFeedbacks(user.getUserId());
            
            res.put("feedbacks", feedbacks);
            res.put("total", feedbacks.size());
            return ResponseEntity.ok(res);
            
        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Lấy chi tiết một phản hồi
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getFeedbackById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long feedbackId
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            FeedbackResponse feedback = feedbackService.getFeedbackById(user.getUserId(), feedbackId);
            
            res.put("feedback", feedback);
            return ResponseEntity.ok(res);
            
        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }
}

