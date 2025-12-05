package com.example.financeapp.log.model;

import java.util.Map;

/**
 * Payload dùng để ghi lại sự kiện đăng nhập.
 */
public record LoginAuditEvent(
        Long userId,
        String email,
        String ipAddress,
        String userAgent,
        LoginLogStatus status,
        String location,
        String failureReason,
        Map<String, Object> metadata
) {
}
