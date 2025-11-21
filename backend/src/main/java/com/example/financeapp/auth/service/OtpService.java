package com.example.financeapp.service;

import com.example.financeapp.entity.User;
import com.example.financeapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;

@Service
public class OtpService {

    private static final int OTP_EXPIRE_SECONDS = 60;       // 60 giây
    private static final int OTP_MAX_PER_HOUR = 5;          // tối đa 3 lần/h
    private static final int OTP_COOLDOWN_SECONDS = 30;     // phải đợi 30 giây giữa 2 lần gửi

    @Autowired
    private UserRepository userRepository;

    /**
     * Kiểm tra xem user có được phép gửi OTP hay không:
     * - Chưa đủ 30 giây cooldown
     * - Vượt quá 3 lần trong 1 giờ
     */
    public boolean canRequestOtp(User user) {

        LocalDateTime now = LocalDateTime.now();

        // Cooldown 30 giây
        if (user.getOtpLastRequest() != null) {
            long diffSeconds = Duration.between(user.getOtpLastRequest(), now).getSeconds();
            if (diffSeconds < OTP_COOLDOWN_SECONDS) {
                return false;
            }
        }

        // Reset limit sau 1 giờ
        if (user.getOtpLastRequest() == null ||
                Duration.between(user.getOtpLastRequest(), now).toHours() >= 1) {
            user.setOtpRequestCount(0);
            userRepository.save(user);
        }

        // Kiểm tra số lần gửi OTP trong 1 giờ
        return user.getOtpRequestCount() < OTP_MAX_PER_HOUR;
    }

    /**
     * Tạo OTP mới và cập nhật thông tin vào User
     */
    public String generateOtp(User user) {

        if (!canRequestOtp(user)) {
            return null;
        }

        String otp = String.format("%06d", new Random().nextInt(1_000_000));

        user.setVerificationCode(otp);
        user.setCodeGeneratedAt(LocalDateTime.now());
        user.setOtpLastRequest(LocalDateTime.now());

        user.setOtpRequestCount(user.getOtpRequestCount() + 1);

        userRepository.save(user);
        return otp;
    }

    /**
     * Kiểm tra OTP hợp lệ và chưa hết hạn (90 giây)
     */
    public boolean verifyOtp(User user, String otp) {

        if (user.getVerificationCode() == null || otp == null) return false;

        long seconds = Duration.between(user.getCodeGeneratedAt(), LocalDateTime.now()).getSeconds();
        if (seconds > OTP_EXPIRE_SECONDS) {
            return false;
        }

        return otp.equals(user.getVerificationCode());
    }

    /**
     * Xóa OTP sau khi dùng
     */
    public void clearOtp(User user) {
        user.setVerificationCode(null);
        user.setCodeGeneratedAt(null);
        userRepository.save(user);
    }
}
