package com.example.financeapp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Table(name = "budgets")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Hạn mức chi tiêu
    private BigDecimal amount;

    // Tên ngân sách (ví dụ "Ăn uống tháng 11", "Đi lại", v.v.)
    private String name;

    // Ngày bắt đầu – cho phép đặt kỳ tùy chọn
    private LocalDate startDate;

    // Ngày kết thúc
    private LocalDate endDate;

    // Liên kết với user
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // Budget có thể theo ví
    @ManyToOne
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    private String category; // Nếu muốn áp dụng theo danh mục (optional)
}
