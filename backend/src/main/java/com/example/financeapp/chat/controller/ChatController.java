package com.example.financeapp.chat.controller;

import com.example.financeapp.chat.dto.ChatRequest;
import com.example.financeapp.chat.dto.ChatResponse;
import com.example.financeapp.chat.service.ChatService;
import com.example.financeapp.security.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller cho Chat API
 * Endpoint: POST /api/chat
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {
    
    private final ChatService chatService;
    
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }
    
    /**
     * POST /api/chat
     * Nhận vào câu hỏi từ người dùng, gọi Service, và trả về câu trả lời
     * 
     * @param userDetails Thông tin user đang đăng nhập
     * @param request ChatRequest chứa message và history
     * @return ResponseEntity<ChatResponse> chứa câu trả lời từ AI
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ChatRequest request
    ) {
        try {
            // Lấy userId từ userDetails
            Long userId = userDetails != null ? userDetails.getUserId() : null;
            
            // Gọi Service để lấy câu trả lời từ AI
            ChatResponse response = chatService.chat(userId, request);
            
            // Trả về kết quả với ResponseEntity
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } catch (Exception e) {
            // Xử lý lỗi và trả về error response
            ChatResponse errorResponse = new ChatResponse(
                "Xin lỗi, đã xảy ra lỗi khi xử lý yêu cầu. Vui lòng thử lại sau.",
                false,
                e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}

