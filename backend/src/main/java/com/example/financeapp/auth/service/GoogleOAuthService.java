package com.example.financeapp.auth.service;

import com.example.financeapp.exception.ApiException;
import com.example.financeapp.exception.ApiErrorCode;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class GoogleOAuthService {

    private final String clientId;

    public GoogleOAuthService(
            @Value("${mywallet.google.client-id:${spring.security.oauth2.client.registration.google.client-id}}") String clientId
    ) {
        this.clientId = clientId;
    }

    public GoogleUserInfo verifyIdToken(String idToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    new GsonFactory()
            ).setAudience(Collections.singletonList(clientId))
                    .build();

            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                throw new ApiException(
                        ApiErrorCode.GOOGLE_TOKEN_INVALID,
                        "ID Token không hợp lệ!"
                );
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();

            return GoogleUserInfo.builder()
                    .email(payload.getEmail())
                    .name((String) payload.get("name"))
                    .picture((String) payload.get("picture"))
                    .build();

        } catch (ApiException e) {
            // Re-throw ApiException để giữ nguyên error code
            throw e;
        } catch (Exception e) {
            throw new ApiException(
                    ApiErrorCode.GOOGLE_TOKEN_INVALID,
                    "Xác thực Google thất bại: " + e.getMessage()
            );
        }
    }
}

