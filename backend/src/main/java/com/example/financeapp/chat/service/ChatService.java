package com.example.financeapp.chat.service;

import com.example.financeapp.chat.dto.ChatRequest;
import com.example.financeapp.chat.dto.ChatResponse;

import java.util.List;

/**
 * Interface cho ChatService
 */
public interface ChatService {
    
    /**
     * Gửi message đến Gemini API và nhận response
     * @param userId ID của user đang chat
     * @param request ChatRequest chứa message và history
     * @return ChatResponse chứa message từ AI hoặc error
     */
    ChatResponse chat(Long userId, ChatRequest request);
    
    /**
     * Lấy lịch sử chat của user từ database
     * @param userId ID của user
     * @return Danh sách tin nhắn (dạng ChatRequest.ChatMessage)
     */
    List<ChatRequest.ChatMessage> getChatHistory(Long userId);
    
    /**
     * Xóa toàn bộ lịch sử chat của user
     * @param userId ID của user
     */
    void clearChatHistory(Long userId);
}

