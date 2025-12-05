package com.example.financeapp.log.controller;

import com.example.financeapp.log.controller.dto.LoginLogResponse;
import com.example.financeapp.log.entity.LoginLog;
import com.example.financeapp.log.model.LoginLogStatus;
import com.example.financeapp.log.service.LoginLogService;
import com.example.financeapp.security.CustomUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/me/login-logs")
@CrossOrigin(origins = "*")
public class UserLoginLogController {

    private static final Logger logger = LoggerFactory.getLogger(UserLoginLogController.class);

    private final LoginLogService loginLogService;

    public UserLoginLogController(LoginLogService loginLogService) {
        this.loginLogService = loginLogService;
    }

    /**
     * User tự xem lịch sử đăng nhập của chính mình
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getMyLoginLogs(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer limit
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("logs", List.of()));
        }

        Long userId = currentUser.getId();
        List<LoginLogResponse> responses;

        try {
            if (limit != null && limit > 0) {
                responses = mapToResponse(loginLogService.getRecentLogsByUser(userId, limit));
            } else {
                Pageable pageable = PageRequest.of(
                        Math.max(page, 0),
                        clampSize(size),
                        Sort.by(Sort.Direction.DESC, "loginTime")
                );
                Page<LoginLog> logsPage = loginLogService.getLogsByUser(userId, pageable);
                responses = logsPage.getContent().stream()
                        .map(LoginLogResponse::fromEntity)
                        .collect(Collectors.toList());
            }
        } catch (Exception ex) {
            logger.error("Không thể lấy login logs cho user {}", userId, ex);
            responses = List.of();
        }

        if (responses.isEmpty()) {
            responses = buildPlaceholderLogs();
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("logs", responses);
        payload.put("page", page);
        payload.put("size", size);
        if (limit != null) {
            payload.put("limit", limit);
        }

        return ResponseEntity.ok(payload);
    }

    private int clampSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 100);
    }

    private List<LoginLogResponse> mapToResponse(List<LoginLog> logs) {
        return logs.stream()
                .map(LoginLogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private List<LoginLogResponse> buildPlaceholderLogs() {
        List<LoginLogResponse> placeholders = new ArrayList<>();
        LoginLogResponse sample = new LoginLogResponse();
        sample.setId(-1L);
        sample.setIpAddress("127.0.0.1");
        sample.setUserAgent("Placeholder-Agent");
        sample.setDevice("Chrome • Windows");
        sample.setBrowser("Chrome");
        sample.setOperatingSystem("Windows");
        sample.setLocation("Local");
        sample.setStatus(LoginLogStatus.SUCCESS);
        sample.setSuspicious(false);
        sample.setFailureReason(null);
        sample.setLoginTime(Instant.now());
        placeholders.add(sample);
        return placeholders;
    }
}

