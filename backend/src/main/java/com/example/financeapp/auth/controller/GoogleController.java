package com.example.financeapp.auth.controller;

import com.example.financeapp.user.entity.User;
import com.example.financeapp.user.repository.UserRepository;
import com.example.financeapp.config.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/auth/google")
public class GoogleController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/success")
    public String googleLoginSuccess(@AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        String picture = principal.getAttribute("picture");

        Optional<User> existingUser = userRepository.findByEmail(email);

        User user;
        if (existingUser.isPresent()) {
            // User đã tồn tại - cập nhật thông tin nếu cần
            user = existingUser.get();

            // Cập nhật avatar nếu có
            if (picture != null && !picture.equals(user.getAvatar())) {
                user.setAvatar(picture);
            }

            // Đảm bảo enabled = true (Google đã verify)
            if (!user.isEnabled()) {
                user.setEnabled(true);
            }

            // Cập nhật provider nếu chưa có
            if (user.getProvider() == null || user.getProvider().isEmpty()) {
                user.setProvider("google");
            }

            userRepository.save(user);
        } else {
            // Tạo user mới
            user = new User();
            user.setEmail(email);
            user.setFullName(name != null ? name : "Google User");
            user.setPasswordHash(null); // Google user không có password
            user.setProvider("google");
            user.setEnabled(true); // Google đã xác thực rồi
            user.setAvatar(picture);

            user = userRepository.save(user);
        }

        // Tạo JWT token cho user
        String token = jwtUtil.generateToken(user.getEmail());

        return "Bearer " + token;
    }
}
