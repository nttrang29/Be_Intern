package com.example.financeapp.auth.service.impl;

import com.example.financeapp.auth.dto.*;
import com.example.financeapp.auth.model.OtpPurpose;
import com.example.financeapp.auth.model.OtpToken;
import com.example.financeapp.auth.repository.OtpTokenRepository;
import com.example.financeapp.auth.service.AuthService;
import com.example.financeapp.auth.service.GoogleOAuthService;
import com.example.financeapp.auth.service.GoogleUserInfo;
import com.example.financeapp.auth.util.OtpUtil;
import com.example.financeapp.common.service.EmailService;
import com.example.financeapp.config.JwtUtil;
import com.example.financeapp.exception.ApiErrorCode;
import com.example.financeapp.exception.ApiException;
import com.example.financeapp.security.CustomUserDetails;
import com.example.financeapp.security.Role;
import com.example.financeapp.user.entity.User;
import com.example.financeapp.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final int REGISTER_OTP_EXPIRE_SECONDS = 300; // 5 phút
    private static final int FORGOT_OTP_EXPIRE_SECONDS = 300; // 5 phút

    private final UserRepository userRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpUtil otpUtil;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final GoogleOAuthService googleOAuthService;

    public AuthServiceImpl(
            UserRepository userRepository,
            OtpTokenRepository otpTokenRepository,
            PasswordEncoder passwordEncoder,
            OtpUtil otpUtil,
            JwtUtil jwtUtil,
            EmailService emailService,
            GoogleOAuthService googleOAuthService
    ) {
        this.userRepository = userRepository;
        this.otpTokenRepository = otpTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpUtil = otpUtil;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
        this.googleOAuthService = googleOAuthService;
    }

    // 1) REGISTER – REQUEST OTP
    @Override
    @Transactional
    public void registerRequestOtp(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // CHECK EMAIL TỒN TẠI + ĐÃ BỊ XOÁ
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.isDeleted()) {
                throw new ApiException(
                        ApiErrorCode.USER_DELETED,
                        "Tài khoản này đã bị xóa hoặc không hoạt động 30 ngày. Vui lòng liên hệ quản trị viên để mở lại."
                );
            }
            throw new ApiException(
                    ApiErrorCode.EMAIL_ALREADY_EXISTS,
                    "Email đã tồn tại trong hệ thống"
            );
        });

        String otp = otpUtil.generateOtp();
        LocalDateTime expiredAt = LocalDateTime.now().plusSeconds(REGISTER_OTP_EXPIRE_SECONDS);

        otpTokenRepository.deleteByEmailAndPurpose(email, OtpPurpose.REGISTER);

        OtpToken otpToken = new OtpToken();
        otpToken.setEmail(email);
        otpToken.setCode(otp);
        otpToken.setPurpose(OtpPurpose.REGISTER);
        otpToken.setExpiredAt(expiredAt);
        otpToken.setUsed(false);
        otpToken.setCreatedAt(LocalDateTime.now());

        otpTokenRepository.save(otpToken);
        emailService.sendOtpRegisterEmail(email, otp);
    }

    // 2) REGISTER – VERIFY OTP
    @Override
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

        // CHECK LẠI EMAIL TRƯỚC KHI TẠO USER
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

        User user = new User();
        user.setEmail(email);
        user.setFullName(request.getFullName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setProvider("local");
        user.setEnabled(true);
        user.setRole(Role.USER);
        user.setGoogleAccount(false);
        user.setFirstLogin(false);
        user.setLocked(false);
        user.setDeleted(false);

        userRepository.save(user);

        otpToken.setUsed(true);
        otpTokenRepository.save(otpToken);

        return jwtUtil.generateToken(new CustomUserDetails(user));
    }

    // 3) LOGIN THƯỜNG
    @Override
    public LoginResult login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND,
                        "Email không tồn tại trong hệ thống"));

        // bị xóa
        if (user.isDeleted()) {
            throw new ApiException(ApiErrorCode.USER_DELETED,
                    "Tài khoản đã bị xóa hoặc không hoạt động trong 30 ngày",
                    HttpStatus.GONE);
        }

        // bị khóa
        if (user.isLocked()) {
            throw new ApiException(ApiErrorCode.ACCOUNT_LOCKED,
                    "Tài khoản bị khóa", HttpStatus.FORBIDDEN);
        }

        // chưa enabled
        if (!user.isEnabled()) {
            throw new ApiException(ApiErrorCode.INVALID_CREDENTIALS,
                    "Tài khoản chưa được kích hoạt");
        }

        // Google account chưa đặt mật khẩu
        if (user.isGoogleAccount() && (user.getPasswordHash() == null || user.getPasswordHash().isEmpty())) {
            throw new ApiException(ApiErrorCode.GOOGLE_ACCOUNT_ONLY,
                    "Tài khoản Google – hãy đăng nhập Google");
        }

        // sai mật khẩu
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException(ApiErrorCode.INVALID_CREDENTIALS,
                    "Sai mật khẩu");
        }

        // cập nhật hoạt động
        user.setLastActiveAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(new CustomUserDetails(user));
        return new LoginResult(user.getUserId(), token);
    }

    // 4) QUÊN MẬT KHẨU – REQUEST OTP
    @Override
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

        if (user.isGoogleAccount() && user.getPasswordHash() == null) {
            throw new ApiException(ApiErrorCode.GOOGLE_ACCOUNT_ONLY,
                    "Tài khoản Google – không thể reset mật khẩu");
        }

        String otp = otpUtil.generateOtp();
        LocalDateTime expiredAt = LocalDateTime.now().plusSeconds(FORGOT_OTP_EXPIRE_SECONDS);

        otpTokenRepository.deleteByEmailAndPurpose(email, OtpPurpose.FORGOT_PASSWORD);

        OtpToken otpToken = new OtpToken();
        otpToken.setEmail(email);
        otpToken.setCode(otp);
        otpToken.setPurpose(OtpPurpose.FORGOT_PASSWORD);
        otpToken.setExpiredAt(expiredAt);
        otpToken.setUsed(false);
        otpToken.setCreatedAt(LocalDateTime.now());

        otpTokenRepository.save(otpToken);
        emailService.sendOtpResetPasswordEmail(email, otp);
    }

    // 5) VERIFY FORGOT OTP → TRẢ resetToken
    @Override
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

    // 6) RESET PASSWORD
    @Override
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
        if (user.getPasswordHash() != null &&
                passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new ApiException(ApiErrorCode.PASSWORD_SAME_AS_OLD,
                    "Mật khẩu mới trùng mật khẩu cũ");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiredAt(null);

        userRepository.save(user);
    }

    // 7) CHANGE PASSWORD KHI LOGIN
    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request, CustomUserDetails currentUser) {
        User user = currentUser.getUser();

        if (user.isGoogleAccount() && (user.getPasswordHash() == null || user.isFirstLogin())) {
            throw new ApiException(ApiErrorCode.GOOGLE_ACCOUNT_ONLY,
                    "Tài khoản Google chưa đặt mật khẩu lần đầu");
        }

        if (request.getOldPassword() == null || request.getOldPassword().isEmpty()) {
            throw new ApiException(ApiErrorCode.INVALID_CREDENTIALS,
                    "Mật khẩu cũ không được để trống");
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new ApiException(ApiErrorCode.INVALID_CREDENTIALS,
                    "Mật khẩu cũ sai");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new ApiException(ApiErrorCode.PASSWORD_SAME_AS_OLD,
                    "Mật khẩu mới trùng mật khẩu cũ");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setFirstLogin(false);

        userRepository.save(user);
    }

    // 8) LOGIN GOOGLE
    @Override
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
            user = new User();
            user.setEmail(email);
            user.setFullName(info.getName());
            user.setAvatar(info.getPicture());
            user.setGoogleAccount(true);
            user.setFirstLogin(true);
            user.setRole(Role.USER);
            user.setLocked(false);
            user.setDeleted(false);
            user.setEnabled(true);
            user.setProvider("google");
        } else {

            // nếu user đã bị xóa → chặn login
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

        String token = jwtUtil.generateToken(new CustomUserDetails(user));
        return new LoginResult(user.getUserId(), token);
    }

    // 9) SET FIRST PASSWORD (Google)
    @Override
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

        if (user.getPasswordHash() != null &&
                passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new ApiException(ApiErrorCode.PASSWORD_SAME_AS_OLD,
                    "Không được đặt mật khẩu trùng mật khẩu cũ");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setFirstLogin(false);

        userRepository.save(user);
    }
}
