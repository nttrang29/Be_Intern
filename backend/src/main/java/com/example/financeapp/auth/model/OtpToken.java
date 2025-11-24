package com.example.financeapp.auth.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "otp_token")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String code;

    @Enumerated(EnumType.STRING)
    private OtpPurpose purpose;

    private LocalDateTime expiredAt;

    private boolean used;

    private LocalDateTime createdAt;
}