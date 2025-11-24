package com.example.financeapp.auth.entity;

import com.example.financeapp.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String token;  // UUID dùng để reset password

    private String otp; // OTP để xác thực bước 2

    private LocalDateTime otpExpiredAt;

    private LocalDateTime tokenExpiredAt;

    @Enumerated(EnumType.STRING)
    private Status status; // PENDING, VERIFIED, CONSUMED

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    public enum Status {
        PENDING,     // Đã gửi OTP nhưng chưa xác thực
        VERIFIED,    // OTP đã verify — chờ reset password
        CONSUMED     // Token đã dùng, không dùng lại được
    }
}
