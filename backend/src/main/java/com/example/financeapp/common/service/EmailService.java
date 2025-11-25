package com.example.financeapp.common.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    
    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.from:no-reply@financeapp.com}")
    private String defaultFrom;

    @Value("${app.mail.mock:false}")
    private boolean mockMode;

    // Hàm gửi chung
    private void send(String to, String subject, String content) {
        if (mockMode) {
            log.info("[MOCK EMAIL] To: {}\nSubject: {}\nContent:\n{}", to, subject, content);
            return;
        }

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(defaultFrom);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(content);
            mailSender.send(msg);

            log.info("Đã gửi email tới {}", to);
        } catch (Exception ex) {
            log.error("Gửi email thất bại tới " + to, ex);
        }
    }

    // Đăng ký (giữ lại để tương thích)
    public void sendRegistrationVerificationEmail(String to, String code) {
        sendOtpRegisterEmail(to, code);
    }

    // Khôi phục mật khẩu (giữ lại để tương thích)
    public void sendPasswordResetEmail(String to, String code) {
        sendOtpResetPasswordEmail(to, code);
    }

    // Gửi OTP đăng ký
    public void sendOtpRegisterEmail(String email, String otp) {
        String subject = "[FinanceApp] Mã xác thực đăng ký tài khoản";
        String content = "Xin chào,\n\n"
                + "Mã OTP đăng ký tài khoản của bạn là: " + otp + "\n"
                + "Mã có hiệu lực trong 1 phút.\n\n"
                + "Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email.\n\n"
                + "Trân trọng,\nĐội ngũ FinanceApp";
        send(email, subject, content);
    }

    // Gửi OTP quên mật khẩu
    public void sendOtpResetPasswordEmail(String email, String otp) {
        String subject = "[FinanceApp] Mã xác thực đặt lại mật khẩu";
        String content = "Xin chào,\n\n"
                + "Mã OTP đặt lại mật khẩu của bạn là: " + otp + "\n"
                + "Mã có hiệu lực trong 1 phút.\n\n"
                + "Nếu bạn không thực hiện yêu cầu này, vui lòng đổi mật khẩu hoặc liên hệ hỗ trợ.\n\n"
                + "Trân trọng,\nĐội ngũ FinanceApp";
        send(email, subject, content);
    }
}
