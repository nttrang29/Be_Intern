package com.example.financeapp.security;

import com.example.financeapp.config.JwtUtil;
import com.example.financeapp.entity.User;
import com.example.financeapp.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final String frontendCallbackUrl;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public OAuth2LoginSuccessHandler(String frontendCallbackUrl, JwtUtil jwtUtil, UserRepository userRepository) {
        this.frontendCallbackUrl = frontendCallbackUrl;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        // Lấy thông tin user từ Google OAuth2
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String picture = oauth2User.getAttribute("picture");
        
        // Kiểm tra và lưu user vào database
        Optional<User> existingUserOpt = userRepository.findByEmail(email);
        User user;
        
        if (existingUserOpt.isEmpty()) {
            // Tạo user mới nếu chưa tồn tại
            user = new User();
            user.setEmail(email);
            user.setFullName(name != null ? name : "Google User");
            user.setPasswordHash(""); // Không cần password cho OAuth2
            user.setProvider("google");
            user.setEnabled(true); // Google đã xác thực rồi
            user.setAvatar(picture);
            
            userRepository.save(user);
        } else {
            // Cập nhật thông tin nếu cần
            user = existingUserOpt.get();
            
            // Cập nhật avatar nếu có thay đổi
            if (picture != null && !picture.equals(user.getAvatar())) {
                user.setAvatar(picture);
            }
            
            // Cập nhật provider nếu chưa có
            if (user.getProvider() == null || user.getProvider().isEmpty()) {
                user.setProvider("google");
            }
            
            // Đảm bảo tài khoản được enable
            if (!user.isEnabled()) {
                user.setEnabled(true);
            }
            
            userRepository.save(user);
        }
        
        // Tạo JWT token
        String token = jwtUtil.generateToken(email);
        String redirect = frontendCallbackUrl + "?token=" +
                URLEncoder.encode(token, StandardCharsets.UTF_8);
        response.sendRedirect(redirect);
    }
}
