package com.example.financeapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    // Đăng ký
    public void sendRegistrationVerificationEmail(String to, String code) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("Xác minh tài khoản đăng ký");
        msg.setText("Mã xác minh: " + code + "\nHiệu lực 10 phút.");
        mailSender.send(msg);
    }

    // Khôi phục mật khẩu
    public void sendPasswordResetEmail(String to, String code) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("Khôi phục mật khẩu");
        msg.setText("Mã xác thực: " + code + "\nHiệu lực 10 phút.\nBỏ qua nếu không yêu cầu.");
        mailSender.send(msg);
    }

    // Gửi mật khẩu mặc định cho Google users
    public void sendDefaultPasswordEmail(String to, String fullName, String defaultPassword) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("🔐 Mật khẩu mặc định cho tài khoản của bạn");
        
        String content = String.format(
            "Xin chào %s,\n\n" +
            "Bạn đã đăng nhập vào hệ thống bằng tài khoản Google.\n\n" +
            "Để bạn có thể đăng nhập bằng email và mật khẩu, chúng tôi đã tự động tạo một mật khẩu mặc định cho bạn:\n\n" +
            "📧 Email: %s\n" +
            "🔑 Mật khẩu: %s\n\n" +
            "Bạn có thể:\n" +
            "✅ Đăng nhập bằng Google (như bình thường)\n" +
            "✅ Đăng nhập bằng email và mật khẩu mặc định này\n" +
            "✅ Đổi sang mật khẩu tùy chỉnh trong phần \"Hồ sơ\" của bạn\n\n" +
            "⚠️ Lưu ý: Mật khẩu này chỉ dành riêng cho bạn và được tạo dựa trên email của bạn.\n\n" +
            "Trân trọng,\n" +
            "Đội ngũ Finance App",
            fullName != null ? fullName : "bạn",
            to,
            defaultPassword
        );
        
        msg.setText(content);
        mailSender.send(msg);
    }
}
