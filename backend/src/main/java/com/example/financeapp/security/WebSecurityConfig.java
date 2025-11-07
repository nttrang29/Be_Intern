package com.example.financeapp.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class WebSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public WebSecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    @Order(1) // chạy sau oauthChain
    SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ✅ CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ✅ Static / public (nếu dùng)
                        .requestMatchers(
                                "/", "/index.html", "/assets/**", "/static/**", "/css/**", "/js/**", "/images/**"
                        ).permitAll()

                        // ✅ Swagger / OpenAPI (nếu dùng springdoc)
                        .requestMatchers(
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
                        ).permitAll()

                        // ✅ Public Auth APIs
                        .requestMatchers(
                                "/auth/login",
                                "/auth/register",
                                "/auth/verify/**",
                                "/auth/forgot-password",
                                "/auth/reset-password"
                        ).permitAll()

                        // ✅ Các API yêu cầu đăng nhập (Bearer JWT)
                        .requestMatchers(
                                "/auth/me",
                                "/auth/profile",
                                "/auth/change-password/request-otp",
                                "/auth/change-password/confirm",
                                "/auth/change-password/resend-otp"
                        ).authenticated()

                        // (Tuỳ chọn) Cho phép GET "/" nếu bạn muốn test nhanh
                        .requestMatchers(HttpMethod.GET, "/").permitAll()

                        // ✅ còn lại phải có JWT
                        .anyRequest().authenticated()
                )

                // ✅ JWT filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // (Tuỳ chọn) Nếu dùng H2 console:
        // http.headers(h -> h.frameOptions(f -> f.sameOrigin()));

        return http.build();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration conf) throws Exception {
        return conf.getAuthenticationManager();
    }
}
