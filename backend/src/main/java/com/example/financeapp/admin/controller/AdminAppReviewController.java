package com.example.financeapp.admin.controller;

import com.example.financeapp.review.dto.AdminReplyRequest;
import com.example.financeapp.review.dto.AppReviewResponse;
import com.example.financeapp.review.service.AppReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller cho admin quản lý đánh giá ứng dụng
 */
@RestController
@RequestMapping("/admin/app-reviews")
@CrossOrigin(origins = "*")
public class AdminAppReviewController {

    @Autowired
    private AppReviewService appReviewService;

    /**
     * Lấy tất cả đánh giá (cho admin)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllReviews(
            @RequestParam(required = false) String status
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            List<AppReviewResponse> reviews;

            if (status != null && !status.isEmpty()) {
                reviews = appReviewService.getReviewsByStatus(status);
            } else {
                reviews = appReviewService.getAllReviews();
            }

            res.put("reviews", reviews);
            res.put("total", reviews.size());

            // Thêm stats
            Map<String, Object> stats = appReviewService.getReviewStats();
            res.put("stats", stats);

            return ResponseEntity.ok(res);

        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Lấy chi tiết một đánh giá (cho admin)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getReviewById(
            @PathVariable("id") Long reviewId
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            AppReviewResponse review = appReviewService.getReviewById(reviewId);
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
     * Admin phản hồi đánh giá
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/reply")
    public ResponseEntity<Map<String, Object>> replyToReview(
            @PathVariable("id") Long reviewId,
            @Valid @RequestBody AdminReplyRequest request
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            AppReviewResponse review = appReviewService.replyToReview(reviewId, request.getAdminReply());

            res.put("message", "Phản hồi đánh giá thành công");
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
     * Lấy thống kê đánh giá (cho admin)
     */
    @PreAuthorize("hasRole('ADMIN')")
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

    /**
     * Xóa đánh giá (cho admin)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteReview(
            @PathVariable("id") Long reviewId
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            appReviewService.deleteReview(reviewId);
            res.put("message", "Xóa đánh giá thành công");
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

