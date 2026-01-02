package com.example.financeapp.review.service.impl;

import com.example.financeapp.notification.entity.Notification;
import com.example.financeapp.notification.service.NotificationService;
import com.example.financeapp.review.dto.AppReviewResponse;
import com.example.financeapp.review.dto.CreateAppReviewRequest;
import com.example.financeapp.review.entity.AppReview;
import com.example.financeapp.review.repository.AppReviewRepository;
import com.example.financeapp.review.service.AppReviewService;
import com.example.financeapp.user.entity.User;
import com.example.financeapp.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AppReviewServiceImpl implements AppReviewService {

    private static final Logger log = LoggerFactory.getLogger(AppReviewServiceImpl.class);

    @Autowired
    private AppReviewRepository appReviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Override
    @Transactional
    public AppReviewResponse createReview(Long userId, CreateAppReviewRequest request) {
        // Kiểm tra user đã đánh giá chưa
        if (appReviewRepository.existsByUser_UserId(userId)) {
            throw new RuntimeException("Bạn đã gửi đánh giá trước đó. Mỗi người dùng chỉ được đánh giá một lần.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        AppReview review = new AppReview();
        review.setUser(user);
        review.setDisplayName(request.getDisplayName() != null && !request.getDisplayName().trim().isEmpty()
                ? request.getDisplayName().trim()
                : null); // null = "Người dùng ẩn danh"
        review.setRating(request.getRating());
        review.setContent(request.getContent());
        review.setStatus(AppReview.ReviewStatus.PENDING);

        AppReview saved = appReviewRepository.save(review);

        // Gửi thông báo cho admin
        notificationService.createAdminNotification(
                Notification.NotificationType.NEW_APP_REVIEW,
                "Đánh giá ứng dụng mới",
                "Bạn có đánh giá ứng dụng mới từ " + 
                        (saved.getDisplayName() != null ? saved.getDisplayName() : "Người dùng ẩn danh") +
                        " (" + saved.getRating() + " sao)",
                saved.getReviewId(),
                "APP_REVIEW"
        );

        log.info("User {} đã gửi đánh giá ứng dụng: {} sao", user.getEmail(), request.getRating());

        return AppReviewResponse.fromEntity(saved);
    }

    @Override
    public List<AppReviewResponse> getAllReviews() {
        List<AppReview> reviews = appReviewRepository.findAllByOrderByCreatedAtDesc();
        return reviews.stream()
                .map(AppReviewResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<AppReviewResponse> getReviewsByStatus(String status) {
        AppReview.ReviewStatus reviewStatus;
        try {
            reviewStatus = AppReview.ReviewStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Trạng thái không hợp lệ: " + status);
        }

        List<AppReview> reviews = appReviewRepository.findByStatusOrderByCreatedAtDesc(reviewStatus);
        return reviews.stream()
                .map(AppReviewResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public AppReviewResponse getUserReview(Long userId) {
        AppReview review = appReviewRepository.findByUser_UserId(userId)
                .orElse(null);

        return review != null ? AppReviewResponse.fromEntity(review) : null;
    }

    @Override
    public AppReviewResponse getReviewById(Long reviewId) {
        AppReview review = appReviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));

        return AppReviewResponse.fromEntity(review);
    }

    @Override
    @Transactional
    public AppReviewResponse replyToReview(Long reviewId, String adminReply) {
        AppReview review = appReviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));

        review.setAdminReply(adminReply);
        review.setRepliedAt(LocalDateTime.now());
        review.setStatus(AppReview.ReviewStatus.ANSWERED);

        AppReview saved = appReviewRepository.save(review);

        // Gửi thông báo cho user
        notificationService.createUserNotification(
                review.getUser().getUserId(),
                Notification.NotificationType.REVIEW_REPLIED,
                "Admin đã phản hồi đánh giá của bạn",
                "Admin đã phản hồi đánh giá ứng dụng của bạn. Nhấn để xem chi tiết.",
                saved.getReviewId(),
                "APP_REVIEW"
        );

        log.info("Admin đã phản hồi đánh giá ID: {}", reviewId);

        return AppReviewResponse.fromEntity(saved);
    }

    @Override
    public Map<String, Object> getReviewStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalReviews = appReviewRepository.count();
        long pendingCount = appReviewRepository.countByStatus(AppReview.ReviewStatus.PENDING);
        long answeredCount = appReviewRepository.countByStatus(AppReview.ReviewStatus.ANSWERED);
        Double averageRating = appReviewRepository.calculateAverageRating();
        long repliedCount = appReviewRepository.countRepliedReviews();

        stats.put("totalReviews", totalReviews);
        stats.put("pendingCount", pendingCount);
        stats.put("answeredCount", answeredCount);
        stats.put("averageRating", averageRating != null ? Math.round(averageRating * 10.0) / 10.0 : 0.0);
        stats.put("repliedCount", repliedCount);

        return stats;
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        AppReview review = appReviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));

        appReviewRepository.delete(review);
        log.info("Đã xóa đánh giá ID: {}", reviewId);
    }
}

