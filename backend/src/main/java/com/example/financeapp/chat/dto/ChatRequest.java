package com.example.financeapp.chat.dto;

import java.util.List;

/**
 * DTO cho request gửi đến Gemini API
 * Format: { "contents": [{ "parts": [{"text": "Câu hỏi của user"}] }] }
 */
public class ChatRequest {
    
    private String message;
    private List<ChatMessage> history;
    
    // Constructor
    public ChatRequest() {
    }
    
    public ChatRequest(String message, List<ChatMessage> history) {
        this.message = message;
        this.history = history;
    }
    
    // Getters and Setters
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public List<ChatMessage> getHistory() {
        return history;
    }
    
    public void setHistory(List<ChatMessage> history) {
        this.history = history;
    }
    
    /**
     * Inner class cho ChatMessage trong history
     */
    public static class ChatMessage {
        private String role; // "user" or "model"
        private String content;
        
        public ChatMessage() {
        }
        
        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
    }
}

