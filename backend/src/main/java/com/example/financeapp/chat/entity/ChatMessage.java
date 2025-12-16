package com.example.financeapp.chat.entity;

import com.example.financeapp.user.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity cho lưu trữ lịch sử chat với AI
 * Mỗi user có lịch sử chat riêng
 */
@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_user_created", columnList = "user_id, created_at")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_message_id")
    private Long chatMessageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "passwordHash", "provider",
            "enabled", "locked", "deleted", "resetToken", "resetTokenExpiredAt"})
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private MessageRole role; // "user" hoặc "model" (AI)

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content; // Nội dung tin nhắn

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Enum cho role của message
    public enum MessageRole {
        USER,   // Tin nhắn từ người dùng
        MODEL   // Tin nhắn từ AI (Gemini)
    }

    // Constructors
    public ChatMessage() {
    }

    public ChatMessage(User user, MessageRole role, String content) {
        this.user = user;
        this.role = role;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getChatMessageId() {
        return chatMessageId;
    }

    public void setChatMessageId(Long chatMessageId) {
        this.chatMessageId = chatMessageId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public MessageRole getRole() {
        return role;
    }

    public void setRole(MessageRole role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

