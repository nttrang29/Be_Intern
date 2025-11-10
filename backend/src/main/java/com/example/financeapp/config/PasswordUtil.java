package com.example.financeapp.config;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Utility class để quản lý mật khẩu mặc định cho Google users
 */
@Component
public class PasswordUtil {
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    /**
     * Tạo mật khẩu mặc định dựa trên email (cho Google users)
     * Format: "Google" + <username_from_email> + "@2024!"
     * VD: john.doe@gmail.com -> GoogleJohndoe@2024!
     */
    public String generateDefaultPassword(String email) {
        if (email == null || !email.contains("@")) {
            return "GoogleDefault@2024!";
        }
        
        // Lấy phần trước @ và loại bỏ ký tự đặc biệt
        String username = email.split("@")[0]
                .replaceAll("[^a-zA-Z0-9]", "")
                .toLowerCase();
        
        // Viết hoa chữ cái đầu
        if (!username.isEmpty()) {
            username = username.substring(0, 1).toUpperCase() + username.substring(1);
        } else {
            username = "User";
        }
        
        return "Google" + username + "@2024!";
    }
    
    /**
     * Kiểm tra xem password có phải là mật khẩu mặc định không
     */
    public boolean isDefaultPassword(String email, String hashedPassword) {
        if (email == null || hashedPassword == null) {
            return false;
        }
        String defaultPassword = generateDefaultPassword(email);
        return passwordEncoder.matches(defaultPassword, hashedPassword);
    }
    
    /**
     * Hash mật khẩu mặc định
     */
    public String hashDefaultPassword(String email) {
        String defaultPassword = generateDefaultPassword(email);
        return passwordEncoder.encode(defaultPassword);
    }
}

