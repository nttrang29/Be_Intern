package com.example.financeapp.chat.repository;

import com.example.financeapp.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository cho ChatMessage entity
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Lấy tất cả tin nhắn của một user, sắp xếp theo thời gian tạo (cũ nhất trước)
     * @param userId ID của user
     * @return Danh sách tin nhắn
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.user.userId = :userId ORDER BY cm.createdAt ASC")
    List<ChatMessage> findByUserIdOrderByCreatedAtAsc(@Param("userId") Long userId);

    /**
     * Xóa tất cả tin nhắn của một user (khi user muốn xóa lịch sử chat)
     * @param userId ID của user
     */
    void deleteByUser_UserId(Long userId);

    /**
     * Đếm số tin nhắn của một user
     * @param userId ID của user
     * @return Số lượng tin nhắn
     */
    long countByUser_UserId(Long userId);
}

