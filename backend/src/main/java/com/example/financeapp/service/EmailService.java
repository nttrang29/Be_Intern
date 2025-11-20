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
}
