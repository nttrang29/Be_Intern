package com.example.financeapp.auth.service;

import com.example.financeapp.auth.dto.*;
import com.example.financeapp.security.CustomUserDetails;

public interface AuthService {
    void registerRequestOtp(RegisterRequest request);
    String verifyRegisterOtp(VerifyOtpRequest request);
    LoginResult login(LoginRequest request);
    void forgotPasswordRequest(ForgotPasswordRequest request);
    String verifyForgotOtp(VerifyForgotOtpRequest request);
    void resetPassword(ResetPasswordRequest request);
    void changePassword(ChangePasswordRequest request, CustomUserDetails currentUser);
    LoginResult loginWithGoogle(GoogleLoginRequest request);
    void setFirstPassword(FirstPasswordRequest request, CustomUserDetails currentUser);
    String verify2FA(Verify2FARequest request);
    void resetTemporary2FA(String email);
}
