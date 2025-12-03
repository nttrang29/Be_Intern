package com.example.financeapp.review.controller;

import com.example.financeapp.review.dto.AppReviewResponse;
import com.example.financeapp.review.dto.CreateAppReviewRequest;
import com.example.financeapp.review.service.AppReviewService;
import com.example.financeapp.security.CustomUserDetails;
import com.example.financeapp.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller cho người dùng đánh giá ứng dụng
 */
@RestController
@RequestMapping("/app-reviews")
@CrossOrigin(origins = "*")
public class AppReviewController {

    @Autowired
    private AppReviewService appReviewService;

    /**
     * Gửi đánh giá ứng dụng
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateAppReviewRequest request
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            AppReviewResponse review = appReviewService.createReview(user.getUserId(), request);

            res.put("message", "Cảm ơn bạn đã đánh giá ứng dụng! Chúng tôi sẽ xem xét và phản hồi sớm nhất có thể.");
            res.put("review", review);
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
     * Lấy đánh giá của user hiện tại
     */
    @GetMapping("/my-review")
    public ResponseEntity<Map<String, Object>> getMyReview(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            AppReviewResponse review = appReviewService.getUserReview(user.getUserId());

            if (review != null) {
                res.put("hasReview", true);
                res.put("review", review);
            } else {
                res.put("hasReview", false);
                res.put("review", null);
            }

            return ResponseEntity.ok(res);

        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Lấy thống kê đánh giá (public)
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getReviewStats() {
        Map<String, Object> res = new HashMap<>();
        try {
            Map<String, Object> stats = appReviewService.getReviewStats();
            res.putAll(stats);
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }
}

