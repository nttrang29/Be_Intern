package com.example.financeapp.feedback.repository;

import com.example.financeapp.feedback.entity.Feedback;
import com.example.financeapp.feedback.entity.FeedbackStatus;
import com.example.financeapp.feedback.entity.FeedbackType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    /**
     * Lấy tất cả feedback của user
     */
    List<Feedback> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Lấy feedback theo status
     */
    List<Feedback> findByStatusOrderByCreatedAtDesc(FeedbackStatus status);

    /**
     * Lấy feedback theo type
     */
    List<Feedback> findByTypeOrderByCreatedAtDesc(FeedbackType type);

    /**
     * Đếm số feedback chưa xử lý (PENDING)
     */
    long countByStatus(FeedbackStatus status);

    /**
     * Lấy feedback của user theo status
     */
    @Query("SELECT f FROM Feedback f WHERE f.user.userId = :userId AND f.status = :status ORDER BY f.createdAt DESC")
    List<Feedback> findByUser_UserIdAndStatusOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("status") FeedbackStatus status
    );
}

