package com.example.financeapp.review.repository;

import com.example.financeapp.review.entity.AppReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppReviewRepository extends JpaRepository<AppReview, Long> {

    /**
     * Lấy tất cả đánh giá, sắp xếp theo thời gian mới nhất
     */
    List<AppReview> findAllByOrderByCreatedAtDesc();

    /**
     * Lấy tất cả đánh giá theo trạng thái
     */
    List<AppReview> findByStatusOrderByCreatedAtDesc(AppReview.ReviewStatus status);

    /**
     * Lấy đánh giá của một user cụ thể
     */
    Optional<AppReview> findByUser_UserId(Long userId);

    /**
     * Kiểm tra user đã đánh giá chưa
     */
    boolean existsByUser_UserId(Long userId);

    /**
     * Đếm số đánh giá theo trạng thái
     */
    long countByStatus(AppReview.ReviewStatus status);

    /**
     * Tính điểm trung bình tất cả đánh giá
     */
    @Query("SELECT AVG(r.rating) FROM AppReview r")
    Double calculateAverageRating();

    /**
     * Đếm tổng số đánh giá
     */
    long count();

    /**
     * Đếm số đánh giá admin đã phản hồi
     */
    @Query("SELECT COUNT(r) FROM AppReview r WHERE r.adminReply IS NOT NULL")
    long countRepliedReviews();
}

