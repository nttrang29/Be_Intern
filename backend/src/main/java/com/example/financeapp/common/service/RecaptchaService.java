package com.example.financeapp.common.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.Map;

@Service
public class RecaptchaService {

    @Value("${app.recaptcha.secret}")
    private String secret;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean verifyToken(String token) {
        // 🧩 Cho phép bỏ qua reCAPTCHA khi đang dev/test
        if ("dev-bypass".equals(token)) {
            System.out.println("[reCAPTCHA] ✅ Bypass token hợp lệ (chế độ dev)");
            return true;
        }

        try {
            // 🧭 Gửi request tới Google API
            String verifyUrl = UriComponentsBuilder
                    .fromHttpUrl("https://www.google.com/recaptcha/api/siteverify")
                    .queryParam("secret", secret)
                    .queryParam("response", token)
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    verifyUrl, HttpMethod.POST, entity, Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Object success = response.getBody().get("success");
                if (Boolean.TRUE.equals(success)) {
                    System.out.println("[reCAPTCHA] ✅ Xác minh thành công");
                    return true;
                } else {
                    System.out.println("[reCAPTCHA] ❌ Thất bại: " + response.getBody());
                }
            }
            return false;
        } catch (Exception e) {
            System.err.println("[reCAPTCHA] ⚠️ Lỗi khi gọi Google API: " + e.getMessage());
            return false;
        }
    }
}
