package com.example.financeapp.security;

import com.example.financeapp.config.JwtUtil;
import com.example.financeapp.user.entity.User;
import com.example.financeapp.user.repository.UserRepository;
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

    public OAuth2LoginSuccessHandler(String frontendCallbackUrl, JwtUtil jwtUtil, 
                                   UserRepository userRepository) {
        this.frontendCallbackUrl = frontendCallbackUrl;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    // Trong file: OAuth2LoginSuccessHandler.java

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String picture = oauth2User.getAttribute("picture");

        Optional<User> existingUserOpt = userRepository.findByEmail(email);
        User user;

        if (existingUserOpt.isEmpty()) {
            // === TRƯỜNG HỢP 1: USER MỚI ===
            // Đây là lần đăng nhập đầu tiên -> Set avatar Google
            user = new User();
            user.setEmail(email);
            user.setFullName(name != null ? name : "Google User");
            user.setPasswordHash(null);
            user.setProvider("google");
            user.setEnabled(true);
            user.setAvatar(picture); // 👈 Set avatar Google

            userRepository.save(user);
        } else {
            // === TRƯỜNG HỢP 2: USER ĐÃ TỒN TẠI ===
            user = existingUserOpt.get();
            boolean needsUpdate = false;

            // ✅ SỬA LỖI LOGIC:

            // 1. Kiểm tra nếu đây là lần đầu họ dùng Google (ví dụ: họ có tk local trước)
            if (user.getProvider() == null || !user.getProvider().equals("google")) {

                // Đặt provider là 'google'
                user.setProvider("google");
                needsUpdate = true;

                // Và đặt avatar Google LÀM MẶC ĐỊNH
                // CHỈ KHI họ chưa từng tự upload avatar
                if (user.getAvatar() == null || user.getAvatar().isEmpty()) {
                    user.setAvatar(picture);
                }
            }

            // 2. Nếu provider đã là 'google' (đây là lần đăng nhập lại)
            // -> TUYỆT ĐỐI KHÔNG GHI ĐÈ avatar.

            // 3. Luôn đảm bảo tài khoản được enable
            if (!user.isEnabled()) {
                user.setEnabled(true);
                needsUpdate = true;
            }

            if (needsUpdate) {
                userRepository.save(user);
            }

            // Nếu đã là user Google cũ và không có gì thay đổi -> không cần save
        }

        // Tạo JWT token và chuyển hướng (như cũ)
        String token = jwtUtil.generateToken(email);
        String redirect = frontendCallbackUrl + "?token=" +
                URLEncoder.encode(token, StandardCharsets.UTF_8);
        response.sendRedirect(redirect);
    }
}
