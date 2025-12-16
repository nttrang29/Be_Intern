package com.example.financeapp.chat.dto;

/**
 * DTO cho response từ Gemini API
 * Format: { "candidates": [{ "content": { "parts": [{"text": "Câu trả lời của AI"}] } }] }
 */
public class ChatResponse {
    
    private String message;
    private boolean success;
    private String error;
    
    // Constructor
    public ChatResponse() {
    }
    
    public ChatResponse(String message, boolean success, String error) {
        this.message = message;
        this.success = success;
        this.error = error;
    }
    
    // Getters and Setters
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
}

