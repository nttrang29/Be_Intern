package com.example.financeapp.admin.controller;

import com.example.financeapp.feedback.dto.FeedbackResponse;
import com.example.financeapp.feedback.entity.Feedback;
import com.example.financeapp.feedback.entity.FeedbackStatus;
import com.example.financeapp.feedback.entity.FeedbackType;
import com.example.financeapp.feedback.repository.FeedbackRepository;
import com.example.financeapp.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/feedbacks")
@CrossOrigin(origins = "*")
public class AdminFeedbackController {

    @Autowired
    private FeedbackRepository feedbackRepository;

    /**
     * Lấy tất cả feedback (cho admin)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllFeedbacks(
            @RequestParam(required = false) FeedbackStatus status,
            @RequestParam(required = false) FeedbackType type
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            List<Feedback> feedbacks;
            
            if (status != null && type != null) {
                feedbacks = feedbackRepository.findAll().stream()
                        .filter(f -> f.getStatus() == status && f.getType() == type)
                        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                        .collect(Collectors.toList());
            } else if (status != null) {
                feedbacks = feedbackRepository.findByStatusOrderByCreatedAtDesc(status);
            } else if (type != null) {
                feedbacks = feedbackRepository.findByTypeOrderByCreatedAtDesc(type);
            } else {
                feedbacks = feedbackRepository.findAll().stream()
                        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                        .collect(Collectors.toList());
            }
            
            List<FeedbackResponse> responses = feedbacks.stream()
                    .map(FeedbackResponse::fromEntity)
                    .collect(Collectors.toList());
            
            res.put("feedbacks", responses);
            res.put("total", responses.size());
            res.put("pendingCount", feedbackRepository.countByStatus(FeedbackStatus.PENDING));
            return ResponseEntity.ok(res);
            
        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Lấy chi tiết một feedback (cho admin)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getFeedbackById(
            @PathVariable("id") Long feedbackId
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            Feedback feedback = feedbackRepository.findById(feedbackId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phản hồi"));
            
            res.put("feedback", FeedbackResponse.fromEntity(feedback));
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
     * Cập nhật trạng thái feedback (cho admin)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateFeedbackStatus(
            @PathVariable("id") Long feedbackId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal CustomUserDetails admin
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            Feedback feedback = feedbackRepository.findById(feedbackId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phản hồi"));
            
            String statusStr = request.get("status");
            if (statusStr == null) {
                throw new RuntimeException("Vui lòng chọn trạng thái");
            }
            
            FeedbackStatus newStatus = FeedbackStatus.valueOf(statusStr.toUpperCase());
            feedback.setStatus(newStatus);
            
            // Cập nhật timestamps
            if (newStatus == FeedbackStatus.REVIEWED && feedback.getReviewedAt() == null) {
                feedback.setReviewedAt(LocalDateTime.now());
            }
            if (newStatus == FeedbackStatus.RESOLVED && feedback.getResolvedAt() == null) {
                feedback.setResolvedAt(LocalDateTime.now());
            }
            
            feedback = feedbackRepository.save(feedback);
            
            res.put("message", "Cập nhật trạng thái thành công");
            res.put("feedback", FeedbackResponse.fromEntity(feedback));
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
     * Thêm phản hồi từ admin
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/response")
    public ResponseEntity<Map<String, Object>> addAdminResponse(
            @PathVariable("id") Long feedbackId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal CustomUserDetails admin
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            Feedback feedback = feedbackRepository.findById(feedbackId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phản hồi"));
            
            String adminResponse = request.get("adminResponse");
            if (adminResponse == null || adminResponse.trim().isEmpty()) {
                throw new RuntimeException("Vui lòng nhập phản hồi");
            }
            
            feedback.setAdminResponse(adminResponse);
            
            // Tự động chuyển sang REVIEWED nếu chưa
            if (feedback.getStatus() == FeedbackStatus.PENDING) {
                feedback.setStatus(FeedbackStatus.REVIEWED);
                feedback.setReviewedAt(LocalDateTime.now());
            }
            
            feedback = feedbackRepository.save(feedback);
            
            res.put("message", "Thêm phản hồi thành công");
            res.put("feedback", FeedbackResponse.fromEntity(feedback));
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
     * Đếm số feedback theo trạng thái (cho dashboard)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getFeedbackStats() {
        Map<String, Object> res = new HashMap<>();
        try {
            long pendingCount = feedbackRepository.countByStatus(FeedbackStatus.PENDING);
            long reviewedCount = feedbackRepository.countByStatus(FeedbackStatus.REVIEWED);
            long resolvedCount = feedbackRepository.countByStatus(FeedbackStatus.RESOLVED);
            long closedCount = feedbackRepository.countByStatus(FeedbackStatus.CLOSED);
            long totalCount = feedbackRepository.count();
            
            res.put("pending", pendingCount);
            res.put("reviewed", reviewedCount);
            res.put("resolved", resolvedCount);
            res.put("closed", closedCount);
            res.put("total", totalCount);
            return ResponseEntity.ok(res);
            
        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }
}

