package com.example.financeapp.admin.controller;

import com.example.financeapp.admin.dto.*;
import com.example.financeapp.admin.service.AdminUserService;
import com.example.financeapp.log.controller.dto.LoginLogResponse;
import com.example.financeapp.log.entity.LoginLog;
import com.example.financeapp.log.service.LoginLogService;
import com.example.financeapp.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/users")
@CrossOrigin(origins = "*")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final LoginLogService loginLogService;

    public AdminUserController(AdminUserService adminUserService, LoginLogService loginLogService) {
        this.adminUserService = adminUserService;
        this.loginLogService = loginLogService;
    }

    // 1) Danh sách user
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> getAllUsers() {
        return ResponseEntity.ok(adminUserService.getAllUsers());
    }

    // 1b) Xem chi tiết 1 user
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/detail")
    public ResponseEntity<AdminUserDetailResponse> getUserDetail(
            @PathVariable("id") Long userId
    ) {
        AdminUserDetailResponse detail = adminUserService.getUserDetail(userId);
        return ResponseEntity.ok(detail);
    }

    // 2) Khóa user
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/lock")
    public ResponseEntity<AdminUserResponse> lockUser(
            @PathVariable("id") Long userId,
            @AuthenticationPrincipal CustomUserDetails admin
    ) {
        AdminUserResponse response = adminUserService.lockUser(userId, admin);
        return ResponseEntity.ok(response);
    }

    // 3) Mở khóa user
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/unlock")
    public ResponseEntity<AdminUserResponse> unlockUser(
            @PathVariable("id") Long userId,
            @AuthenticationPrincipal CustomUserDetails admin
    ) {
        AdminUserResponse response = adminUserService.unlockUser(userId, admin);
        return ResponseEntity.ok(response);
    }

    // 4) Đổi role user
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/role")
    public ResponseEntity<AdminUserResponse> changeRole(
            @PathVariable("id") Long userId,
            @Valid @RequestBody ChangeRoleRequest request,
            @AuthenticationPrincipal CustomUserDetails admin
    ) {
        AdminUserResponse response = adminUserService.changeRole(userId, request, admin);
        return ResponseEntity.ok(response);
    }

    // 5) Xem log hành động admin
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/logs")
    public ResponseEntity<List<AdminActionLogResponse>> getAdminLogs() {
        return ResponseEntity.ok(adminUserService.getAllAdminActionLogs());
    }

    // 6) Xem login logs của 1 user
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/login-logs")
    public ResponseEntity<List<LoginLogResponse>> getUserLoginLogs(
            @PathVariable("id") Long userId
    ) {
        List<LoginLog> logs = loginLogService.getLogsByUser(userId);
        List<LoginLogResponse> result = logs.stream()
                .map(LoginLogResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // 7) Xóa user
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable("id") Long userId,
            @AuthenticationPrincipal CustomUserDetails admin
    ) {
        adminUserService.deleteUser(userId, admin);
        return ResponseEntity.noContent().build();
    }
}

