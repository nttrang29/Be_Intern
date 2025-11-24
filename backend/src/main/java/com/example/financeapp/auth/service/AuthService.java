package com.example.financeapp.auth.service;

import com.example.financeapp.auth.dto.*;
import com.example.financeapp.auth.model.OtpPurpose;
import com.example.financeapp.auth.model.OtpToken;
import com.example.financeapp.auth.repository.OtpTokenRepository;
import com.example.financeapp.auth.util.OtpUtil;
import com.example.financeapp.email.EmailService;
import com.example.financeapp.exception.ApiErrorCode;
import com.example.financeapp.exception.ApiException;
import com.example.financeapp.security.CustomUserDetails;
import com.example.financeapp.security.JwtTokenProvider;
import com.example.financeapp.security.Role;
import com.example.financeapp.user.entity.User;
import com.example.financeapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int REGISTER_OTP_EXPIRE_SECONDS = 60;
    private static final int FORGOT_OTP_EXPIRE_SECONDS  = 60;

    private final UserRepository userRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpUtil otpUtil;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final GoogleOAuthService googleOAuthService;

    // ============================================================
    // 1) REGISTER – REQUEST OTP
    // ============================================================
    @Transactional
    public void registerRequestOtp(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // 🔍 CHECK EMAIL TỒN TẠI + ĐÃ BỊ XOÁ
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.isDeleted()) {
                // ❌ Tài khoản đã bị xoá / không hoạt động 30 ngày → không cho đăng ký lại
                throw new ApiException(
                        ApiErrorCode.USER_DELETED,
                        "Tài khoản này đã bị xóa hoặc không hoạt động 30 ngày. Vui lòng liên hệ quản trị viên để mở lại."
                );
            }

            // ❌ Email vẫn đang dùng trong hệ thống
            throw new ApiException(
                    ApiErrorCode.EMAIL_ALREADY_EXISTS,
                    "Email đã tồn tại trong hệ thống"
            );
        });

        String otp = otpUtil.generateOtp();
        LocalDateTime expiredAt = LocalDateTime.now().plusSeconds(REGISTER_OTP_EXPIRE_SECONDS);

        otpTokenRepository.deleteByEmailAndPurpose(email, OtpPurpose.REGISTER);

        OtpToken otpToken = OtpToken.builder()
                .email(email)
                .code(otp)
                .purpose(OtpPurpose.REGISTER)
                .expiredAt(expiredAt)
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();

        otpTokenRepository.save(otpToken);
        emailService.sendOtpRegisterEmail(email, otp);
    }

    // ============================================================
    // 2) REGISTER – VERIFY OTP
    // ============================================================
    @Transactional
    public String verifyRegisterOtp(VerifyOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        OtpToken otpToken = otpTokenRepository
                .findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                        email, OtpPurpose.REGISTER
                )
                .orElseThrow(() ->
                        new ApiException(
                                ApiErrorCode.OTP_NOT_FOUND,
                                "OTP chưa được tạo hoặc đã dùng"
                        ));

        if (otpToken.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new ApiException(ApiErrorCode.OTP_EXPIRED, "OTP đã hết hạn");
        }

        if (!otpToken.getCode().equals(request.getOtp())) {
            throw new ApiException(ApiErrorCode.OTP_INVALID, "OTP sai");
        }

        // 🔍 CHECK LẠI EMAIL TRƯỚC KHI TẠO USER
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.isDeleted()) {
                throw new ApiException(
                        ApiErrorCode.USER_DELETED,
                        "Tài khoản này đã bị xóa hoặc không hoạt động 30 ngày. Vui lòng liên hệ quản trị viên để mở lại."
                );
            }

            throw new ApiException(
                    ApiErrorCode.EMAIL_ALREADY_EXISTS,
                    "Email đã được đăng ký"
            );
        });

        User user = User.builder()
                .email(email)
                .fullName(request.getFullName())  // nếu muốn thì .trim() thêm cũng được
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .googleAccount(false)
                .firstLogin(false)
                .locked(false)
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .lastActiveAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        otpToken.setUsed(true);
        otpTokenRepository.save(otpToken);

        return jwtTokenProvider.generateToken(new CustomUserDetails(user));
    }

    // ============================================================
    // 3) LOGIN THƯỜNG
    // ============================================================
    public LoginResult login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND,
                        "Email không tồn tại trong hệ thống"));

        // ❌ bị xóa
        if (user.isDeleted()) {
            throw new ApiException(ApiErrorCode.USER_DELETED,
                    "Tài khoản đã bị xóa hoặc không hoạt động trong 30 ngày",
                    HttpStatus.GONE);
        }

        // 🔒 bị khóa
        if (user.isLocked()) {
            throw new ApiException(ApiErrorCode.ACCOUNT_LOCKED,
                    "Tài khoản bị khóa", HttpStatus.FORBIDDEN);
        }

        // ❌ Google account chưa đặt mật khẩu
        if (user.isGoogleAccount() && (user.getPassword() == null || user.getPassword().isEmpty())) {
            throw new ApiException(ApiErrorCode.GOOGLE_ACCOUNT_ONLY,
                    "Tài khoản Google – hãy đăng nhập Google");
        }

        // ❌ sai mật khẩu
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ApiException(ApiErrorCode.INVALID_CREDENTIALS,
                    "Sai mật khẩu");
        }

        // ⭐ cập nhật hoạt động
        user.setLastActiveAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(new CustomUserDetails(user));
        return new LoginResult(user.getId(), token);
    }

    // ============================================================
    // 4) QUÊN MẬT KHẨU – REQUEST OTP
    // ============================================================
    @Transactional
    public void forgotPasswordRequest(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND,
                        "Email không tồn tại"));

        if (user.isDeleted()) {
            throw new ApiException(ApiErrorCode.USER_DELETED,
                    "Tài khoản đã bị xóa hoặc không hoạt động 30 ngày");
        }

        if (user.isGoogleAccount() && user.getPassword() == null) {
            throw new ApiException(ApiErrorCode.GOOGLE_ACCOUNT_ONLY,
                    "Tài khoản Google – không thể reset mật khẩu");
        }

        String otp = otpUtil.generateOtp();
        LocalDateTime expiredAt = LocalDateTime.now().plusSeconds(FORGOT_OTP_EXPIRE_SECONDS);

        otpTokenRepository.deleteByEmailAndPurpose(email, OtpPurpose.FORGOT_PASSWORD);

        OtpToken otpToken = OtpToken.builder()
                .email(email)
                .code(otp)
                .purpose(OtpPurpose.FORGOT_PASSWORD)
                .expiredAt(expiredAt)
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();

        otpTokenRepository.save(otpToken);
        emailService.sendOtpResetPasswordEmail(email, otp);
    }

    // ============================================================
    // 5) VERIFY FORGOT OTP → TRẢ resetToken
    // ============================================================
    @Transactional
    public String verifyForgotOtp(VerifyForgotOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ApiException(ApiErrorCode.USER_NOT_FOUND, "Email không tồn tại"));

        if (user.isDeleted()) {
            throw new ApiException(ApiErrorCode.USER_DELETED,
                    "Tài khoản đã bị xóa hoặc không hoạt động 30 ngày");
        }

        OtpToken otpToken = otpTokenRepository
                .findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                        email, OtpPurpose.FORGOT_PASSWORD
                )
                .orElseThrow(() ->
                        new ApiException(ApiErrorCode.OTP_NOT_FOUND,
                                "OTP chưa được tạo hoặc đã dùng"));

        if (otpToken.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new ApiException(ApiErrorCode.OTP_EXPIRED, "OTP đã hết hạn");
        }

        if (!otpToken.getCode().equals(request.getOtp())) {
            throw new ApiException(ApiErrorCode.OTP_INVALID, "OTP sai");
        }

        String resetToken = UUID.randomUUID().toString();
        user.setResetToken(resetToken);
        user.setResetTokenExpiredAt(LocalDateTime.now().plusMinutes(15));

        userRepository.save(user);

        otpToken.setUsed(true);
        otpTokenRepository.save(otpToken);

        return resetToken;
    }

    // ============================================================
    // 6) RESET PASSWORD
    // ============================================================
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetToken(request.getResetToken())
                .orElseThrow(() ->
                        new ApiException(ApiErrorCode.INVALID_RESET_TOKEN,
                                "Reset token không hợp lệ"));

        if (user.getResetTokenExpiredAt() == null ||
                user.getResetTokenExpiredAt().isBefore(LocalDateTime.now())) {
            throw new ApiException(ApiErrorCode.RESET_TOKEN_EXPIRED,
                    "Reset token hết hạn");
        }

        // không cho đặt giống mật khẩu cũ
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new ApiException(ApiErrorCode.PASSWORD_SAME_AS_OLD,
                    "Mật khẩu mới trùng mật khẩu cũ");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiredAt(null);

        userRepository.save(user);
    }

    // ============================================================
    // 7) CHANGE PASSWORD KHI LOGIN
    // ============================================================
    @Transactional
    public void changePassword(ChangePasswordRequest request, CustomUserDetails currentUser) {
        User user = currentUser.getUser();

        if (user.isGoogleAccount() && (user.getPassword() == null || user.isFirstLogin())) {
            throw new ApiException(ApiErrorCode.GOOGLE_ACCOUNT_ONLY,
                    "Tài khoản Google chưa đặt mật khẩu lần đầu");
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new ApiException(ApiErrorCode.INVALID_CREDENTIALS,
                    "Mật khẩu cũ sai");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new ApiException(ApiErrorCode.PASSWORD_SAME_AS_OLD,
                    "Mật khẩu mới trùng mật khẩu cũ");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setFirstLogin(false);

        userRepository.save(user);
    }

    // ============================================================
    // 8) LOGIN GOOGLE
    // ============================================================
    @Transactional
    public LoginResult loginWithGoogle(GoogleLoginRequest request) {

        GoogleUserInfo info = googleOAuthService.verifyIdToken(request.getIdToken());

        if (info == null || info.getEmail() == null) {
            throw new ApiException(ApiErrorCode.GOOGLE_TOKEN_INVALID,
                    "Google token không hợp lệ");
        }

        String email = info.getEmail().toLowerCase();
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            // Lần đầu login Google
            user = User.builder()
                    .email(email)
                    .fullName(info.getName())
                    .avatar(info.getPicture())
                    .googleAccount(true)
                    .firstLogin(true)
                    .role(Role.USER)
                    .locked(false)
                    .deleted(false)
                    .createdAt(LocalDateTime.now())
                    .lastActiveAt(LocalDateTime.now())
                    .build();
        } else {

            // ❌ nếu user đã bị xóa → chặn login
            if (user.isDeleted()) {
                throw new ApiException(ApiErrorCode.USER_DELETED,
                        "Tài khoản đã bị xóa hoặc không hoạt động 30 ngày");
            }

            if (user.isLocked()) {
                throw new ApiException(ApiErrorCode.ACCOUNT_LOCKED,
                        "Tài khoản bị khóa");
            }

            // Cập nhật avatar nếu rỗng
            if ((user.getAvatar() == null || user.getAvatar().isBlank())
                    && info.getPicture() != null) {
                user.setAvatar(info.getPicture());
            }

            // Cập nhật name nếu rỗng
            if ((user.getFullName() == null || user.getFullName().isBlank())
                    && info.getName() != null) {
                user.setFullName(info.getName());
            }

            user.setGoogleAccount(true);
            user.setLastActiveAt(LocalDateTime.now());
        }

        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(new CustomUserDetails(user));
        return new LoginResult(user.getId(), token);
    }

    // ============================================================
    // 9) SET FIRST PASSWORD (Google)
    // ============================================================
    @Transactional
    public void setFirstPassword(FirstPasswordRequest request, CustomUserDetails currentUser) {

        User user = currentUser.getUser();

        if (!user.isGoogleAccount()) {
            throw new ApiException(ApiErrorCode.GOOGLE_ACCOUNT_ONLY,
                    "Chỉ tài khoản Google mới được đặt mật khẩu lần đầu");
        }

        if (!user.isFirstLogin()) {
            throw new ApiException(ApiErrorCode.INVALID_CREDENTIALS,
                    "Tài khoản đã có mật khẩu, không phải lần đầu");
        }

        if (user.getPassword() != null &&
                passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new ApiException(ApiErrorCode.PASSWORD_SAME_AS_OLD,
                    "Không được đặt mật khẩu trùng mật khẩu cũ");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setFirstLogin(false);

        userRepository.save(user);
    }
}
