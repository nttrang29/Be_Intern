package com.example.financeapp.security;

import com.example.financeapp.config.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                try {
                    // Sử dụng extractEmailSafely để tránh throw exception khi token expired
                    String email = jwtUtil.extractEmailSafely(jwt);

                    if (email != null && jwtUtil.validateToken(jwt, email)) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );

                        authentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } else {
                        // Token không hợp lệ hoặc đã hết hạn
                        if (jwtUtil.isTokenExpired(jwt)) {
                            log.debug("JWT token đã hết hạn cho request: {}", request.getRequestURI());
                        } else {
                            log.debug("JWT token không hợp lệ cho request: {}", request.getRequestURI());
                        }
                    }
                } catch (ExpiredJwtException ex) {
                    // Token đã hết hạn - log nhưng không throw để request vẫn tiếp tục
                    // Spring Security sẽ xử lý authentication failure sau đó
                    log.debug("JWT token đã hết hạn: {}", ex.getMessage());
                } catch (Exception ex) {
                    // Các lỗi khác (malformed token, etc.)
                    log.debug("Lỗi xử lý JWT token: {}", ex.getMessage());
                }
            }
        } catch (Exception ex) {
            // Lỗi không mong đợi - log nhưng không throw để tránh vỡ flow
            log.error("Không thể thiết lập xác thực cho người dùng trong SecurityContext", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) &&
                bearerToken.toLowerCase().startsWith("bearer ")) {
            return bearerToken.substring(7).trim();
        }
        return null;
    }
}