package com.example.financeapp.log.controller;

import com.example.financeapp.log.controller.dto.LoginLogResponse;
import com.example.financeapp.log.entity.LoginLog;
import com.example.financeapp.log.service.LoginLogService;
import com.example.financeapp.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/me/login-logs")
@CrossOrigin(origins = "*")
public class UserLoginLogController {

    private final LoginLogService loginLogService;

    public UserLoginLogController(LoginLogService loginLogService) {
        this.loginLogService = loginLogService;
    }

    /**
     * User tự xem lịch sử đăng nhập của chính mình
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LoginLogResponse>> getMyLoginLogs(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        Long userId = currentUser.getId();

        List<LoginLog> logs = loginLogService.getLogsByUser(userId);

        List<LoginLogResponse> result = logs.stream()
                .map(LoginLogResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}

