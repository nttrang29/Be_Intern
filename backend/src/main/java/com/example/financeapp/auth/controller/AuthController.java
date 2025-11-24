package com.example.financeapp.auth.controller;

import com.example.financeapp.auth.dto.*;
import com.example.financeapp.auth.service.AuthService;
import com.example.financeapp.log.service.LoginLogService;          // 👈 THÊM
import com.example.financeapp.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;          // 👈 THÊM
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    private final LoginLogService loginLogService;       // 👈 THÊM

    // ===================================================
    // 1) Đăng ký: yêu cầu OTP
    // ===================================================
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

    // ===================================================
    // 3) Đăng nhập thường
    // ===================================================
    @PostMapping("/login")
    public ResponseEntity<?> login(
            HttpServletRequest servletRequest,                 // 👈 THÊM để lấy IP + User-Agent
            @Valid @RequestBody LoginRequest request
    ) {
        // 👇 BƯỚC 1: login, lấy về cả token + userId
        LoginResult result = authService.login(request);

        // 👇 BƯỚC 2: lấy IP + User-Agent
        String ip = servletRequest.getRemoteAddr();
        String userAgent = servletRequest.getHeader("User-Agent");

        // 👇 BƯỚC 3: lưu nhật ký đăng nhập
        loginLogService.save(result.getUserId(), ip, userAgent);

        // 👇 BƯỚC 4: trả token cho FE (giữ nguyên format cũ)
        return ResponseEntity.ok(new AuthTokenResponse(result.getToken()));
    }

    // ===================================================
    // 4) QUÊN MẬT KHẨU – Bước 1: Gửi OTP
    // ===================================================
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPasswordRequest(request);
        return ResponseEntity.ok(new SimpleMessageResponse("Đã gửi OTP đặt lại mật khẩu"));
    }

    // ===================================================
    // 5) QUÊN MẬT KHẨU – Bước 2: Xác nhận OTP → trả resetToken
    // ===================================================
    @PostMapping("/verify-forgot-otp")
    public ResponseEntity<?> verifyForgotOtp(
            @Valid @RequestBody VerifyForgotOtpRequest request
    ) {
        String resetToken = authService.verifyForgotOtp(request);
        return ResponseEntity.ok(new ResetTokenResponse(resetToken));
    }

    // ===================================================
    // 6) QUÊN MẬT KHẨU – Bước 3: Đổi mật khẩu bằng resetToken
    // ===================================================
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authService.resetPassword(request);
        return ResponseEntity.ok(new SimpleMessageResponse("Đổi mật khẩu thành công"));
    }

    // ===================================================
    // 7) Đổi mật khẩu khi đã đăng nhập
    // ===================================================
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

    // ===================================================
    // 9) ĐẶT MẬT KHẨU LẦN ĐẦU CHO TÀI KHOẢN GOOGLE
    // ===================================================
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

    // ===================================================
    // 8) Đăng nhập bằng Google
    // ===================================================
    @PostMapping("/google-login")
    public ResponseEntity<?> loginWithGoogle(
            HttpServletRequest servletRequest,                 // 👈 THÊM
            @Valid @RequestBody GoogleLoginRequest request
    ) {
        // BƯỚC 1: login Google, lấy userId + token
        LoginResult result = authService.loginWithGoogle(request);

        // BƯỚC 2: log lại IP + thiết bị
        String ip = servletRequest.getRemoteAddr();
        String userAgent = servletRequest.getHeader("User-Agent");
        loginLogService.save(result.getUserId(), ip, userAgent);

        // BƯỚC 3: trả token cho FE
        return ResponseEntity.ok(new AuthTokenResponse(result.getToken()));
    }

    // =============== RESPONSE DTO ===============
    record SimpleMessageResponse(String message) {}
    record AuthTokenResponse(String token) {}
    record ResetTokenResponse(String resetToken) {}
}
