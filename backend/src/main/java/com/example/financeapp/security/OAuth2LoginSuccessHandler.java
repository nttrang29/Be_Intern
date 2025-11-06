package com.example.financeapp.security;

import com.example.financeapp.config.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final String frontendCallbackUrl;
    private final JwtUtil jwtUtil;

    public OAuth2LoginSuccessHandler(String frontendCallbackUrl, JwtUtil jwtUtil) {
        this.frontendCallbackUrl = frontendCallbackUrl;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        // Tạo JWT từ username (tuỳ JwtUtil của bạn)
        String token = jwtUtil.generateToken(authentication.getName());
        String redirect = frontendCallbackUrl + "?token=" +
                URLEncoder.encode(token, StandardCharsets.UTF_8);
        response.sendRedirect(redirect);
    }
}
