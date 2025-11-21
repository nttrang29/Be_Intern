package com.example.financeapp.controller;

import com.example.financeapp.dto.ChangePasswordRequest;
import com.example.financeapp.dto.UpdateProfileRequest;
import com.example.financeapp.entity.User;
import com.example.financeapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
}

