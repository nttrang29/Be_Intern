package com.example.financeapp.chat.controller;

import com.example.financeapp.chat.dto.ChatRequest;
import com.example.financeapp.chat.dto.ChatResponse;
import com.example.financeapp.chat.service.ChatService;
import com.example.financeapp.security.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller cho Chat API
 * Endpoints:
 * - POST /api/chat - Gửi tin nhắn và nhận phản hồi từ AI
 * - GET /api/chat/history - Lấy lịch sử chat
 * - DELETE /api/chat/history - Xóa lịch sử chat
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
            
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new ChatResponse("Bạn cần đăng nhập để sử dụng tính năng chat.", false, "Unauthorized")
                );
            }
            
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
    
    /**
     * GET /api/chat/history
     * Lấy lịch sử chat của user từ database
     * 
     * @param userDetails Thông tin user đang đăng nhập
     * @return ResponseEntity chứa danh sách tin nhắn
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getChatHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            Long userId = userDetails != null ? userDetails.getUserId() : null;
            
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Lấy lịch sử chat từ database
            List<ChatRequest.ChatMessage> history = chatService.getChatHistory(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("history", history);
            response.put("count", history.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("history", List.of());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * DELETE /api/chat/history
     * Xóa toàn bộ lịch sử chat của user
     * 
     * @param userDetails Thông tin user đang đăng nhập
     * @return ResponseEntity chứa kết quả
     */
    @DeleteMapping("/history")
    public ResponseEntity<Map<String, Object>> clearChatHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            Long userId = userDetails != null ? userDetails.getUserId() : null;
            
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Xóa lịch sử chat
            chatService.clearChatHistory(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã xóa lịch sử chat thành công.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}

