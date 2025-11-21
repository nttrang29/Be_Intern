package com.example.financeapp.auth.controller;

import com.example.financeapp.config.JwtUtil;
import com.example.financeapp.auth.dto.LoginRequest;
import com.example.financeapp.user.entity.User;
import com.example.financeapp.user.repository.UserRepository;
import com.example.financeapp.common.service.EmailService;
import com.example.financeapp.common.service.RecaptchaService;
import com.example.financeapp.common.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired private UserRepository userRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private EmailService emailService;
    @Autowired private RecaptchaService recaptchaService;
    @Autowired private OtpService otpService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ================================
    // 🚨 HÀM KIỂM TRA ĐỘ MẠNH MẬT KHẨU
    // ================================
    private boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if ("!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(c) >= 0) hasSpecial = true;
        }
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    // ================================
    // 🟢 REGISTER (có CAPTCHA)
    // ================================
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> req) {

        Map<String, Object> res = new HashMap<>();
        String fullName = req.get("fullName");
        String email = req.get("email");
        String password = req.get("password");
        String confirm = req.get("confirmPassword");
        String captcha = req.get("recaptchaToken");

        if (fullName == null || email == null || password == null || confirm == null || captcha == null) {
            res.put("error", "Thiếu thông tin đăng ký");
            return res;
        }
        if (!isStrongPassword(password)) {
            res.put("error", "Mật khẩu yếu: phải ≥8 ký tự và chứa hoa - thường - số - ký tự đặc biệt");
            return res;
        }
        if (!password.equals(confirm)) {
            res.put("error", "Mật khẩu xác nhận không khớp");
            return res;
        }

        Optional<User> existOpt = userRepository.findByEmail(email);

        // Tài khoản tồn tại nhưng chưa kích hoạt → cho đăng ký lại
        if (existOpt.isPresent() && !existOpt.get().isEnabled()) {
            User u = existOpt.get();
            u.setFullName(fullName);
            u.setPasswordHash(passwordEncoder.encode(password));

            String otp = otpService.generateOtp(u);
            emailService.sendRegistrationVerificationEmail(email, otp);

            res.put("message", "Đăng ký lại thành công. Vui lòng kiểm tra email.");
            return res;
        }

        // Tài khoản đã kích hoạt → báo lỗi
        if (existOpt.isPresent()) {
            res.put("error", "Email đã tồn tại. Vui lòng đăng nhập.");
            return res;
        }

        // Tạo tài khoản mới
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setProvider("local");
        user.setEnabled(false);
        userRepository.save(user);

        // Gửi OTP
        String otp = otpService.generateOtp(user);
        emailService.sendRegistrationVerificationEmail(email, otp);

        res.put("message", "Đăng ký thành công. Vui lòng kiểm tra email để xác minh.");
        return res;
    }

    // ================================
    // 🟡 VERIFY EMAIL
    // ================================
    @PostMapping("/verify")
    public Map<String, Object> verify(@RequestBody Map<String, String> req) {

        Map<String, Object> res = new HashMap<>();
        String email = req.get("email");
        String code = req.get("code");

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            res.put("error", "Email không tồn tại");
            return res;
        }

        if (user.isEnabled()) {
            res.put("message", "Tài khoản đã được xác minh");
            return res;
        }

        if (!otpService.verifyOtp(user, code)) {
            res.put("error", "OTP sai hoặc đã hết hạn");
            return res;
        }

        user.setEnabled(true);
        otpService.clearOtp(user);

        String access = jwtUtil.generateToken(email);
        String refresh = jwtUtil.generateRefreshToken(email);

        res.put("message", "Xác minh thành công");
        res.put("accessToken", access);
        res.put("refreshToken", refresh);
        return res;
    }

    // ================================
    // 🟢 LOGIN
    // ================================
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest req) {

        Map<String, Object> res = new HashMap<>();

        User user = userRepository.findByEmail(req.getEmail()).orElse(null);
        if (user == null || !user.isEnabled()) {
            res.put("error", "Tài khoản hoặc mật khẩu không đúng");
            return res;
        }

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            res.put("error", "Tài khoản đăng nhập Google, vui lòng dùng Google");
            return res;
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            res.put("error", "Tài khoản hoặc mật khẩu không đúng");
            return res;
        }

        res.put("message", "Đăng nhập thành công");
        res.put("accessToken", jwtUtil.generateToken(user.getEmail()));
        res.put("refreshToken", jwtUtil.generateRefreshToken(user.getEmail()));
        res.put("user", user);
        return res;
    }

    // ================================
    // 🔄 REFRESH TOKEN
    // ================================
    @PostMapping("/refresh")
    public Map<String, Object> refresh(@RequestBody Map<String, String> req) {

        Map<String, Object> res = new HashMap<>();
        try {
            String refresh = req.get("refreshToken");
            String email = jwtUtil.extractEmail(refresh);

            if (!jwtUtil.validateToken(refresh, email)) {
                res.put("error", "Refresh token không hợp lệ");
                return res;
            }

            res.put("accessToken", jwtUtil.generateToken(email));
            res.put("message", "Làm mới token thành công");
            return res;

        } catch (Exception e) {
            res.put("error", "Token lỗi");
            return res;
        }
    }

    // ================================
    // 🔵 FORGOT PASSWORD → GỬI OTP
    // ================================
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPass(@RequestBody Map<String, String> req) {

        Map<String, Object> res = new HashMap<>();
        String email = req.get("email");

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null || !user.isEnabled()) {
            res.put("error", "Email không hợp lệ");
            return ResponseEntity.badRequest().body(res);
        }

        if (!otpService.canRequestOtp(user)) {
            res.put("error", "Bạn gửi OTP quá nhiều (3 lần/h) hoặc quá nhanh (cooldown 30s)");
            return ResponseEntity.badRequest().body(res);
        }

        String otp = otpService.generateOtp(user);
        emailService.sendPasswordResetEmail(email, otp);

        res.put("message", "OTP đã được gửi");
        return ResponseEntity.ok(res);
    }

    // ================================
    // 🟡 VERIFY OTP FOR RESET PASSWORD
    // ================================
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody Map<String, String> req) {

        Map<String, Object> res = new HashMap<>();
        String email = req.get("email");
        String otp = req.get("otp");

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || !user.isEnabled()) {
            res.put("error", "Tài khoản không tồn tại");
            return ResponseEntity.badRequest().body(res);
        }

        if (!otpService.verifyOtp(user, otp)) {
            res.put("error", "OTP sai hoặc hết hạn");
            return ResponseEntity.badRequest().body(res);
        }

        res.put("message", "OTP hợp lệ");
        return ResponseEntity.ok(res);
    }

    // ================================
    // 🔴 RESET PASSWORD
    // ================================
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> req) {

        Map<String, Object> res = new HashMap<>();
        String email = req.get("email");
        String otp = req.get("otp");
        String newPass = req.get("newPassword");
        String confirm = req.get("confirmPassword");

        if (!newPass.equals(confirm)) {
            res.put("error", "Xác nhận mật khẩu không khớp");
            return ResponseEntity.badRequest().body(res);
        }

        if (!isStrongPassword(newPass)) {
            res.put("error", "Mật khẩu yếu");
            return ResponseEntity.badRequest().body(res);
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            res.put("error", "Email không tồn tại");
            return ResponseEntity.badRequest().body(res);
        }

        if (!otpService.verifyOtp(user, otp)) {
            res.put("error", "OTP sai hoặc hết hạn");
            return ResponseEntity.badRequest().body(res);
        }

        user.setPasswordHash(passwordEncoder.encode(newPass));
        otpService.clearOtp(user);

        res.put("message", "Đổi mật khẩu thành công");
        return ResponseEntity.ok(res);
    }

    // ================================
    // 🔵 RESEND VERIFICATION (KHI ĐĂNG KÝ)
    // ================================
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, Object>> resendVerify(@RequestBody Map<String, String> req) {

        Map<String, Object> res = new HashMap<>();
        String email = req.get("email");

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            res.put("error", "Email không tồn tại");
            return ResponseEntity.badRequest().body(res);
        }

        if (user.isEnabled()) {
            res.put("error", "Tài khoản đã được kích hoạt");
            return ResponseEntity.badRequest().body(res);
        }

        if (!otpService.canRequestOtp(user)) {
            res.put("error", "Bạn gửi OTP quá nhanh hoặc vượt quá 3 lần/h");
            return ResponseEntity.badRequest().body(res);
        }

        String otp = otpService.generateOtp(user);
        emailService.sendRegistrationVerificationEmail(email, otp);

        res.put("message", "Đã gửi lại mã xác minh");
        return ResponseEntity.ok(res);
    }
}
