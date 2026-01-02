package com.example.financeapp.log.controller;

import com.example.financeapp.common.dto.PagedResponse;
import com.example.financeapp.log.controller.dto.LoginLogResponse;
import com.example.financeapp.log.entity.LoginLog;
import com.example.financeapp.log.model.LoginLogStatus;
import com.example.financeapp.log.service.LoginLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/login-logs")
@CrossOrigin(origins = "*")
public class AdminLoginLogController {

    private final LoginLogService loginLogService;

    public AdminLoginLogController(LoginLogService loginLogService) {
        this.loginLogService = loginLogService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<PagedResponse<LoginLogResponse>> getLoginLogs(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "ip", required = false) String ipAddress,
            @RequestParam(value = "status", required = false) LoginLogStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                clampSize(size),
                Sort.by(Sort.Direction.DESC, "loginTime")
        );

        Page<LoginLog> logs = loginLogService.searchLogs(userId, ipAddress, status, pageable);
        Page<LoginLogResponse> mapped = logs.map(LoginLogResponse::fromEntity);
        return ResponseEntity.ok(PagedResponse.from(mapped));
    }

    private int clampSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 200);
    }
}
