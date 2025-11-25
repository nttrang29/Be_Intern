package com.example.financeapp.email;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:no-reply@mywallet.com}")
    private String defaultFrom;

    // nếu bạn muốn chế độ "mock" (chỉ log, không gửi), set = true khi dev
    @Value("${app.mail.mock:false}")
    private boolean mockMode;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // Hàm gửi chung
    private void send(String to, String subject, String content) {
        if (mockMode) {
            // CHẾ ĐỘ MOCK: chỉ log, không gửi mail thật
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
            // Không ném exception để tránh làm fail flow chính (đăng ký/otp),
            // chỉ log và cho FE biết là "đã gửi", thực tế nên log để debug.
            log.error("Gửi email thất bại tới " + to, ex);
        }
    }

    // ====== Gửi OTP đăng ký ======
    public void sendOtpRegisterEmail(String email, String otp) {
        String subject = "[MyWallet] Mã xác thực đăng ký tài khoản";
        String content = "Xin chào,\n\n"
                + "Mã OTP đăng ký tài khoản MyWallet của bạn là: " + otp + "\n"
                + "Mã có hiệu lực trong 5 phút.\n\n"
                + "Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email.\n\n"
                + "Trân trọng,\nĐội ngũ MyWallet";
        send(email, subject, content);
    }

    // ====== Gửi OTP quên mật khẩu ======
    public void sendOtpResetPasswordEmail(String email, String otp) {
        String subject = "[MyWallet] Mã xác thực đặt lại mật khẩu";
        String content = "Xin chào,\n\n"
                + "Mã OTP đặt lại mật khẩu MyWallet của bạn là: " + otp + "\n"
                + "Mã có hiệu lực trong 5 phút.\n\n"
                + "Nếu bạn không thực hiện yêu cầu này, vui lòng đổi mật khẩu hoặc liên hệ hỗ trợ.\n\n"
                + "Trân trọng,\nĐội ngũ MyWallet";
        send(email, subject, content);
    }

    // ====== Gửi nhắc nhở ghi giao dịch hàng ngày ======
    public void sendDailyReminderEmail(String email, String fullName) {
        String subject = "[MyWallet] Nhắc nhở ghi giao dịch hôm nay";
        String content = "Xin chào " + fullName + ",\n\n"
                + "Bạn chưa ghi giao dịch nào hôm nay. Hãy nhớ ghi chép thu chi để quản lý tài chính tốt hơn nhé!\n\n"
                + "📝 Ghi chép ngay: Đăng nhập vào ứng dụng và thêm giao dịch mới.\n\n"
                + "Nếu bạn đã ghi giao dịch, vui lòng bỏ qua email này.\n\n"
                + "Trân trọng,\nĐội ngũ MyWallet";
        send(email, subject, content);
    }

    // ====== Gửi thông báo feedback mới cho admin ======
    public void sendFeedbackNotificationEmail(String adminEmail, String userName, String userEmail, 
                                             String feedbackType, String subject, String message) {
        String emailSubject = "[MyWallet] Phản hồi mới từ người dùng: " + subject;
        String emailContent = "Xin chào Admin,\n\n"
                + "Có phản hồi mới từ người dùng:\n\n"
                + "Người gửi: " + userName + " (" + userEmail + ")\n"
                + "Loại: " + feedbackType + "\n"
                + "Tiêu đề: " + subject + "\n"
                + "Nội dung:\n" + message + "\n\n"
                + "Vui lòng đăng nhập vào hệ thống quản trị để xem và xử lý phản hồi này.\n\n"
                + "Trân trọng,\nHệ thống MyWallet";
        send(adminEmail, emailSubject, emailContent);
    }
}

