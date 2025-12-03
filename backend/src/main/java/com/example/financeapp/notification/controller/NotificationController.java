package com.example.financeapp.notification.controller;

import com.example.financeapp.notification.dto.NotificationResponse;
import com.example.financeapp.notification.service.NotificationService;
import com.example.financeapp.security.CustomUserDetails;
import com.example.financeapp.security.Role;
import com.example.financeapp.user.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller cho hệ thống thông báo
 */
@RestController
@RequestMapping("/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * Lấy tất cả thông báo của user hoặc admin
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            List<NotificationResponse> notifications;

            if (user.getRole() == Role.ADMIN) {
                notifications = notificationService.getAdminNotifications();
            } else {
                notifications = notificationService.getUserNotifications(user.getUserId());
            }

            res.put("notifications", notifications);
            res.put("total", notifications.size());
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Lấy thông báo chưa đọc
     */
    @GetMapping("/unread")
    public ResponseEntity<Map<String, Object>> getUnreadNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            List<NotificationResponse> notifications;

            if (user.getRole() == Role.ADMIN) {
                notifications = notificationService.getUnreadAdminNotifications();
            } else {
                notifications = notificationService.getUnreadUserNotifications(user.getUserId());
            }

            res.put("notifications", notifications);
            res.put("total", notifications.size());
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Đếm số thông báo chưa đọc
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            String role = user.getRole() == Role.ADMIN ? "ADMIN" : "USER";

            long count = notificationService.countUnreadNotifications(user.getUserId(), role);

            res.put("unreadCount", count);
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Đánh dấu thông báo đã đọc
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long notificationId
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            String role = user.getRole() == Role.ADMIN ? "ADMIN" : "USER";

            NotificationResponse notification = notificationService.markAsRead(
                    notificationId, user.getUserId(), role);

            res.put("message", "Đã đánh dấu đã đọc");
            res.put("notification", notification);
            return ResponseEntity.ok(res);

        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Đánh dấu tất cả thông báo đã đọc
     */
    @PutMapping("/mark-all-read")
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            String role = user.getRole() == Role.ADMIN ? "ADMIN" : "USER";

            notificationService.markAllAsRead(user.getUserId(), role);

            res.put("message", "Đã đánh dấu tất cả thông báo là đã đọc");
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Xóa thông báo
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long notificationId
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            String role = user.getRole() == Role.ADMIN ? "ADMIN" : "USER";

            notificationService.deleteNotification(notificationId, user.getUserId(), role);

            res.put("message", "Xóa thông báo thành công");
            return ResponseEntity.ok(res);

        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }
}

