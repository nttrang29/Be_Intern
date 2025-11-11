package com.example.financeapp.config;

import com.example.financeapp.repository.UserRepository;
import com.example.financeapp.security.OAuth2LoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class OAuth2SecurityConfig {

    private final JwtUtil jwtUtil;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final UserRepository userRepository;

    public OAuth2SecurityConfig(JwtUtil jwtUtil, 
                                ClientRegistrationRepository clientRegistrationRepository,
                                UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.userRepository = userRepository;
    }

    @Bean
    @Order(0)
    SecurityFilterChain oauthChain(HttpSecurity http) throws Exception {
        String FE = "http://localhost:3000"; // FE thật của bạn hiện đang chạy 3000
        var successHandler = new OAuth2LoginSuccessHandler(FE + "/oauth/callback", jwtUtil, userRepository);

        var customResolver = new CustomOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository,
                "/auth/oauth2/authorization" // baseUri bạn đang dùng ở FE
        );

        http
                .securityMatcher("/auth/oauth2/**", "/oauth2/**", "/login/oauth2/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .oauth2Login(o -> o
                        .authorizationEndpoint(ep -> ep
                                .baseUri("/auth/oauth2/authorization")
                                .authorizationRequestResolver(customResolver) // 👈 gắn vào đây
                        )
                        .redirectionEndpoint(r -> r.baseUri("/login/oauth2/code/*"))
                        .successHandler(successHandler)
                );

        return http.build();
    }
}
