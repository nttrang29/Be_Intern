package com.example.financeapp.chat.service;

import com.example.financeapp.chat.dto.ChatRequest;
import com.example.financeapp.chat.dto.ChatResponse;

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
}

