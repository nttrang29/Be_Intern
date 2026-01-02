package com.example.financeapp.user.controller;

import com.example.financeapp.auth.dto.ChangePasswordRequest;
import com.example.financeapp.user.dto.Change2FARequest;
import com.example.financeapp.user.dto.Setup2FARequest;
import com.example.financeapp.user.dto.UpdateProfileRequest;
import com.example.financeapp.user.entity.User;
import com.example.financeapp.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/profile")
@CrossOrigin(origins = "*")
public class ProfileController {

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Kiểm tra mật khẩu có đủ mạnh không
     */
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

    /**
     * Lấy user hiện tại từ token
     */
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElse(null);
    }

    // -----------------------------
    // 👤 XEM THÔNG TIN PROFILE
    // -----------------------------
    @GetMapping
    public ResponseEntity<Map<String, Object>> getProfile() {
        Map<String, Object> res = new HashMap<>();

        User user = getCurrentUser();
        if (user == null) {
            res.put("error", "Không tìm thấy thông tin người dùng");
            return ResponseEntity.status(401).body(res);
        }

        // Tạo response với thông tin user (không trả về password)
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.getUserId());
        userInfo.put("fullName", user.getFullName());
        userInfo.put("email", user.getEmail());
        userInfo.put("provider", user.getProvider());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("enabled", user.isEnabled());
        // Thêm role để frontend biết quyền của user
        userInfo.put("role", user.getRole() != null ? user.getRole().name() : "USER");
        // Thêm thông tin về việc user đã có password chưa
        boolean hasPassword = user.getPasswordHash() != null && !user.getPasswordHash().trim().isEmpty();
        userInfo.put("hasPassword", hasPassword);

        res.put("user", userInfo);
        return ResponseEntity.ok(res);
    }

    // -----------------------------
    // ✏️ CẬP NHẬT PROFILE
    // -----------------------------
    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody UpdateProfileRequest request) {
        Map<String, Object> res = new HashMap<>();

        User user = getCurrentUser();
        if (user == null) {
            res.put("error", "Không tìm thấy thông tin người dùng");
            return ResponseEntity.status(401).body(res);
        }

        // Cập nhật fullName nếu có
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName().trim());
        }

        // Cập nhật avatar nếu có
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        userRepository.save(user);

        res.put("message", "Cập nhật profile thành công");
        res.put("user", user);
        return ResponseEntity.ok(res);
    }

    // -----------------------------
    // 🔐 ĐỔI MẬT KHẨU
    // -----------------------------
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody ChangePasswordRequest request) {
        Map<String, Object> res = new HashMap<>();

        User user = getCurrentUser();
        if (user == null) {
            res.put("error", "Không tìm thấy thông tin người dùng");
            return ResponseEntity.status(401).body(res);
        }

        // Kiểm tra mật khẩu mới và confirm password
        if (request.getNewPassword() == null || request.getConfirmPassword() == null) {
            res.put("error", "Vui lòng nhập đầy đủ mật khẩu mới và xác nhận mật khẩu");
            return ResponseEntity.badRequest().body(res);
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            res.put("error", "Mật khẩu mới và xác nhận không khớp");
            return ResponseEntity.badRequest().body(res);
        }

        // Kiểm tra độ mạnh mật khẩu
        if (!isStrongPassword(request.getNewPassword())) {
            res.put("error", "Mật khẩu phải ≥8 ký tự, có chữ hoa, thường, số, ký tự đặc biệt");
            return ResponseEntity.badRequest().body(res);
        }

        // Logic đổi mật khẩu:
        // - Nếu user chưa có password (Google user, passwordHash = null) → ĐẶT mật khẩu lần đầu
        // - Nếu đã có password → CẦN old password để đổi

        if (user.getPasswordHash() == null || user.getPasswordHash().trim().isEmpty()) {
            // Trường hợp 1: User chưa có password (Google user đặt password lần đầu)
            // Không cần kiểm tra old password
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            res.put("message", "Đặt mật khẩu thành công. Bây giờ bạn có thể đăng nhập bằng email và mật khẩu.");
            return ResponseEntity.ok(res);
        } else {
            // Trường hợp 2: Đổi mật khẩu (đã có password)
            // BẮT BUỘC phải có old password

            if (request.getOldPassword() == null || request.getOldPassword().trim().isEmpty()) {
                res.put("error", "Vui lòng nhập mật khẩu hiện tại");
                return ResponseEntity.badRequest().body(res);
            }

            // Kiểm tra old password có đúng không
            if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
                res.put("error", "Mật khẩu hiện tại không đúng");
                return ResponseEntity.badRequest().body(res);
            }

            // Đổi mật khẩu
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            res.put("message", "Đổi mật khẩu thành công");
            return ResponseEntity.ok(res);
        }
    }

    // -----------------------------
    // 🔐 XÁC THỰC 2 LỚP (2FA)
    // -----------------------------

    // GET /profile/2fa/status - Kiểm tra trạng thái 2FA
    @GetMapping("/2fa/status")
    public ResponseEntity<Map<String, Object>> get2FAStatus() {
        Map<String, Object> res = new HashMap<>();

        User user = getCurrentUser();
        if (user == null) {
            res.put("error", "Không tìm thấy thông tin người dùng");
            return ResponseEntity.status(401).body(res);
        }

        boolean enabled = user.isTwoFactorEnabled();
        boolean hasSecret = user.getTwoFactorSecret() != null && !user.getTwoFactorSecret().isEmpty();

        res.put("enabled", enabled);
        res.put("hasSecret", hasSecret);
        return ResponseEntity.ok(res);
    }

    // POST /profile/2fa/setup - Setup 2FA (user tự tạo mã pin)
    @PostMapping("/2fa/setup")
    @Transactional
    public ResponseEntity<Map<String, Object>> setup2FA(@RequestBody Setup2FARequest request) {
        Map<String, Object> res = new HashMap<>();

        User user = getCurrentUser();
        if (user == null) {
            res.put("error", "Không tìm thấy thông tin người dùng");
            return ResponseEntity.status(401).body(res);
        }

        // Nếu đã có secret, không cho setup lại
        if (user.getTwoFactorSecret() != null && !user.getTwoFactorSecret().isEmpty()) {
            res.put("error", "Bạn đã setup 2FA rồi. Vui lòng bật/tắt thay vì setup lại.");
            return ResponseEntity.badRequest().body(res);
        }

        // Kiểm tra mã pin do user tạo
        if (request == null || request.getCode() == null || request.getCode().trim().isEmpty()) {
            res.put("error", "Vui lòng nhập mã pin 6 số");
            return ResponseEntity.badRequest().body(res);
        }

        String code = request.getCode().trim();
        if (code.length() != 6 || !code.matches("\\d{6}")) {
            res.put("error", "Mã pin phải là 6 chữ số");
            return ResponseEntity.badRequest().body(res);
        }

        // Lưu mã pin đã hash vào user
        user.setTwoFactorSecret(passwordEncoder.encode(code));
        userRepository.save(user);

        res.put("message", "Đã tạo mã pin 2FA thành công. Vui lòng bật xác thực 2 lớp.");
        return ResponseEntity.ok(res);
    }

    // POST /profile/2fa/enable - Bật 2FA
    @PostMapping("/2fa/enable")
    @Transactional
    public ResponseEntity<Map<String, Object>> enable2FA(@RequestBody(required = false) Setup2FARequest request) {
        Map<String, Object> res = new HashMap<>();

        User user = getCurrentUser();
        if (user == null) {
            res.put("error", "Không tìm thấy thông tin người dùng");
            return ResponseEntity.status(401).body(res);
        }

        // Nếu đã bật rồi
        if (user.isTwoFactorEnabled()) {
            res.put("message", "Xác thực 2 lớp đã được bật");
            return ResponseEntity.ok(res);
        }

        // Nếu chưa có secret (chưa setup), cần setup trước
        if (user.getTwoFactorSecret() == null || user.getTwoFactorSecret().isEmpty()) {
            res.put("error", "Vui lòng setup mã pin 2FA trước khi bật");
            return ResponseEntity.badRequest().body(res);
        }

        // Nếu có secret rồi, chỉ cần bật (không cần xác nhận lại)
        // Bật 2FA
        user.setTwoFactorEnabled(true);
        userRepository.save(user);

        res.put("message", "Đã bật xác thực 2 lớp thành công");
        return ResponseEntity.ok(res);
    }

    // POST /profile/2fa/disable - Tắt 2FA
    @PostMapping("/2fa/disable")
    @Transactional
    public ResponseEntity<Map<String, Object>> disable2FA() {
        Map<String, Object> res = new HashMap<>();

        User user = getCurrentUser();
        if (user == null) {
            res.put("error", "Không tìm thấy thông tin người dùng");
            return ResponseEntity.status(401).body(res);
        }

        // Tắt 2FA
        user.setTwoFactorEnabled(false);
        // Không xóa secret để user có thể bật lại mà không cần setup lại
        userRepository.save(user);

        res.put("message", "Đã tắt xác thực 2 lớp thành công");
        return ResponseEntity.ok(res);
    }

    // POST /profile/2fa/change - Đổi mã xác thực 2FA
    @PostMapping("/2fa/change")
    @Transactional
    public ResponseEntity<Map<String, Object>> change2FA(@RequestBody Change2FARequest request) {
        Map<String, Object> res = new HashMap<>();

        User user = getCurrentUser();
        if (user == null) {
            res.put("error", "Không tìm thấy thông tin người dùng");
            return ResponseEntity.status(401).body(res);
        }

        if (!user.isTwoFactorEnabled()) {
            res.put("error", "Tài khoản chưa bật xác thực 2 lớp");
            return ResponseEntity.badRequest().body(res);
        }

        // Kiểm tra các trường bắt buộc
        if (request.getOldCode() == null || request.getOldCode().trim().isEmpty()) {
            res.put("error", "Vui lòng nhập mã xác thực cũ");
            return ResponseEntity.badRequest().body(res);
        }

        if (request.getNewCode() == null || request.getNewCode().trim().isEmpty()) {
            res.put("error", "Vui lòng nhập mã xác thực mới");
            return ResponseEntity.badRequest().body(res);
        }

        if (request.getConfirmCode() == null || request.getConfirmCode().trim().isEmpty()) {
            res.put("error", "Vui lòng nhập lại mã xác thực mới");
            return ResponseEntity.badRequest().body(res);
        }

        String oldCode = request.getOldCode().trim();
        String newCode = request.getNewCode().trim();
        String confirmCode = request.getConfirmCode().trim();

        // Kiểm tra mã mới phải là 6 số
        if (newCode.length() != 6 || !newCode.matches("\\d{6}")) {
            res.put("error", "Mã xác thực mới phải là 6 chữ số");
            return ResponseEntity.badRequest().body(res);
        }

        // Kiểm tra mã mới và nhập lại phải khớp
        if (!newCode.equals(confirmCode)) {
            res.put("error", "Mã xác thực mới và nhập lại không khớp");
            return ResponseEntity.badRequest().body(res);
        }

        // Kiểm tra mã cũ có đúng không
        if (user.getTwoFactorSecret() == null || user.getTwoFactorSecret().isEmpty()) {
            res.put("error", "Mã pin 2FA chưa được thiết lập");
            return ResponseEntity.badRequest().body(res);
        }

        if (!passwordEncoder.matches(oldCode, user.getTwoFactorSecret())) {
            res.put("error", "Mã xác thực cũ không đúng");
            return ResponseEntity.badRequest().body(res);
        }

        // Kiểm tra mã mới không được trùng với mã cũ
        if (passwordEncoder.matches(newCode, user.getTwoFactorSecret())) {
            res.put("error", "Mã xác thực mới không được trùng với mã cũ");
            return ResponseEntity.badRequest().body(res);
        }

        // Lưu mã pin mới đã hash
        user.setTwoFactorSecret(passwordEncoder.encode(newCode));
        userRepository.save(user);

        res.put("message", "Đã đổi mã xác thực 2 lớp thành công");
        return ResponseEntity.ok(res);
    }
}

