package com.example.financeapp.feedback.service.impl;

import com.example.financeapp.email.EmailService;
import com.example.financeapp.feedback.dto.CreateFeedbackRequest;
import com.example.financeapp.feedback.dto.FeedbackResponse;
import com.example.financeapp.feedback.entity.Feedback;
import com.example.financeapp.feedback.entity.FeedbackStatus;
import com.example.financeapp.feedback.repository.FeedbackRepository;
import com.example.financeapp.feedback.service.FeedbackService;
import com.example.financeapp.user.entity.User;
import com.example.financeapp.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FeedbackServiceImpl implements FeedbackService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackServiceImpl.class);

    @Autowired
    private FeedbackRepository feedbackRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Value("${app.admin.email:admin@financeapp.com}")
    private String adminEmail;

    @Override
    @Transactional
    public FeedbackResponse createFeedback(Long userId, CreateFeedbackRequest request) {
        // 1. Kiểm tra user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // 2. Tạo feedback
        Feedback feedback = new Feedback();
        feedback.setUser(user);
        feedback.setType(request.getType());
        feedback.setStatus(FeedbackStatus.PENDING);
        feedback.setSubject(request.getSubject());
        feedback.setMessage(request.getMessage());
        feedback.setContactEmail(request.getContactEmail() != null ? request.getContactEmail() : user.getEmail());

        feedback = feedbackRepository.save(feedback);

        // 3. Gửi email thông báo cho admin
        try {
            String typeName = switch (request.getType()) {
                case FEEDBACK -> "Phản hồi";
                case BUG -> "Báo lỗi";
                case FEATURE -> "Đề xuất tính năng";
                case OTHER -> "Khác";
            };
            
            emailService.sendFeedbackNotificationEmail(
                    adminEmail,
                    user.getFullName(),
                    feedback.getContactEmail(),
                    typeName,
                    request.getSubject(),
                    request.getMessage()
            );
            
            log.info("Đã gửi thông báo feedback mới cho admin: {}", adminEmail);
        } catch (Exception e) {
            // Không throw exception, chỉ log lỗi
            log.error("Lỗi khi gửi email thông báo feedback cho admin: {}", e.getMessage(), e);
        }

        log.info("User {} đã gửi feedback: {}", user.getEmail(), request.getSubject());

        return FeedbackResponse.fromEntity(feedback);
    }

    @Override
    public List<FeedbackResponse> getUserFeedbacks(Long userId) {
        List<Feedback> feedbacks = feedbackRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);
        return feedbacks.stream()
                .map(FeedbackResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public FeedbackResponse getFeedbackById(Long userId, Long feedbackId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phản hồi"));

        // Kiểm tra quyền: chỉ user tạo feedback mới được xem
        if (!feedback.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xem phản hồi này");
        }

        return FeedbackResponse.fromEntity(feedback);
    }
}

