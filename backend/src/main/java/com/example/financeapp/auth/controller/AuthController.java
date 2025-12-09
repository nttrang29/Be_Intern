package com.example.financeapp.auth.controller;

import com.example.financeapp.auth.dto.*;
import com.example.financeapp.auth.service.AuthService;
import com.example.financeapp.log.model.LoginAuditEvent;
import com.example.financeapp.log.model.LoginLogStatus;
import com.example.financeapp.log.service.LoginLogService;
import com.example.financeapp.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final LoginLogService loginLogService;

    public AuthController(AuthService authService, LoginLogService loginLogService) {
        this.authService = authService;
        this.loginLogService = loginLogService;
    }

    // 1) Đăng ký: yêu cầu OTP
    @PostMapping("/register-request-otp")
    public ResponseEntity<?> registerRequestOtp(@Valid @RequestBody RegisterRequest request) {
        authService.registerRequestOtp(request);
        return ResponseEntity.ok(new SimpleMessageResponse("Đã gửi mã OTP tới email, vui lòng kiểm tra hộp thư"));
    }

    // 2) Đăng ký: xác thực OTP + tạo tài khoản
    @PostMapping("/verify-register-otp")
    public ResponseEntity<?> verifyRegisterOtp(@Valid @RequestBody VerifyOtpRequest request) {
        String token = authService.verifyRegisterOtp(request);
        return ResponseEntity.ok(new AuthTokenResponse(token));
    }

    // 3) Đăng nhập thường
    @PostMapping("/login")
    public ResponseEntity<?> login(
            HttpServletRequest servletRequest,
            @Valid @RequestBody LoginRequest request
    ) {
        String ip = extractClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        try {
            LoginResult result = authService.login(request);
            recordLoginAttempt(
                    result.getUserId(),
                    request.getEmail(),
                    ip,
                    userAgent,
                    LoginLogStatus.SUCCESS,
                    null,
                    Collections.singletonMap("provider", "local")
            );

            // Trả về response với requires2FA
            Map<String, Object> response = new HashMap<>();
            response.put("token", result.getToken());
            response.put("requires2FA", result.isRequires2FA());
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            recordLoginAttempt(
                    null,
                    request.getEmail(),
                    ip,
                    userAgent,
                    LoginLogStatus.FAILURE,
                    ex.getMessage(),
                    Collections.singletonMap("provider", "local")
            );
            throw ex;
        }
    }

    // 4) QUÊN MẬT KHẨU – Bước 1: Gửi OTP
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPasswordRequest(request);
        return ResponseEntity.ok(new SimpleMessageResponse("Đã gửi OTP đặt lại mật khẩu"));
    }

    // 5) QUÊN MẬT KHẨU – Bước 2: Xác nhận OTP → trả resetToken
    @PostMapping("/verify-forgot-otp")
    public ResponseEntity<?> verifyForgotOtp(
            @Valid @RequestBody VerifyForgotOtpRequest request
    ) {
        String resetToken = authService.verifyForgotOtp(request);
        return ResponseEntity.ok(new ResetTokenResponse(resetToken));
    }

    // 6) QUÊN MẬT KHẨU – Bước 3: Đổi mật khẩu bằng resetToken
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authService.resetPassword(request);
        return ResponseEntity.ok(new SimpleMessageResponse("Đổi mật khẩu thành công"));
    }

    // 7) Đổi mật khẩu khi đã đăng nhập
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(401)
                    .body(new SimpleMessageResponse("Bạn chưa đăng nhập"));
        }

        authService.changePassword(request, currentUser);
        return ResponseEntity.ok(new SimpleMessageResponse("Đổi mật khẩu thành công"));
    }

    // 8) ĐẶT MẬT KHẨU LẦN ĐẦU CHO TÀI KHOẢN GOOGLE
    @PostMapping("/set-first-password")
    public ResponseEntity<?> setFirstPassword(
            @Valid @RequestBody FirstPasswordRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(401)
                    .body(new SimpleMessageResponse("Bạn chưa đăng nhập"));
        }

        authService.setFirstPassword(request, currentUser);
        return ResponseEntity.ok(new SimpleMessageResponse("Đặt mật khẩu lần đầu thành công"));
    }

    // 9) Đăng nhập bằng Google
    @PostMapping("/google-login")
    public ResponseEntity<?> loginWithGoogle(
            HttpServletRequest servletRequest,
            @Valid @RequestBody GoogleLoginRequest request
    ) {
        String ip = extractClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        try {
            LoginResult result = authService.loginWithGoogle(request);
            recordLoginAttempt(
                    result.getUserId(),
                    null,
                    ip,
                    userAgent,
                    LoginLogStatus.SUCCESS,
                    null,
                    Collections.singletonMap("provider", "google")
            );
            // Google login cũng cần kiểm tra 2FA
            Map<String, Object> response = new HashMap<>();
            response.put("token", result.getToken());
            response.put("requires2FA", result.isRequires2FA());
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            recordLoginAttempt(
                    null,
                    null,
                    ip,
                    userAgent,
                    LoginLogStatus.FAILURE,
                    ex.getMessage(),
                    Collections.singletonMap("provider", "google")
            );
            throw ex;
        }
    }

    // 10) Xác thực 2FA sau khi login
    @PostMapping("/verify-2fa")
    public ResponseEntity<?> verify2FA(@Valid @RequestBody Verify2FARequest request) {
        String token = authService.verify2FA(request);
        return ResponseEntity.ok(new AuthTokenResponse(token));
    }

    // 11) Reset mã 2FA tạm thời (khi quên mã) - không cần authentication
    @PostMapping("/reset-2fa-temporary")
    public ResponseEntity<?> resetTemporary2FA(@Valid @RequestBody Verify2FARequest request) {
        authService.resetTemporary2FA(request.getEmail());
        return ResponseEntity.ok(new SimpleMessageResponse("Đã gửi mã xác thực tạm thời tới email của bạn"));
    }

    private void recordLoginAttempt(
            Long userId,
            String email,
            String ip,
            String userAgent,
            LoginLogStatus status,
            String failureReason,
            Map<String, Object> metadata
    ) {
        LoginAuditEvent event = new LoginAuditEvent(
                userId,
                email,
                ip,
                userAgent,
                status,
                null,
                failureReason,
                metadata
        );
        try {
            loginLogService.recordEvent(event);
        } catch (RuntimeException ex) {
            logger.warn("Không thể ghi nhận login log", ex);
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // =============== RESPONSE DTO ===============
    record SimpleMessageResponse(String message) {}
    record AuthTokenResponse(String token) {}
    record ResetTokenResponse(String resetToken) {}
}
