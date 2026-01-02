package com.example.financeapp.fund.entity;

import com.example.financeapp.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity cho thành viên quỹ nhóm
 */
@Entity
@Table(name = "fund_members", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"fund_id", "user_id"})
})
public class FundMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fund_id", nullable = false)
    private Fund fund;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private FundMemberRole role;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    // Getters & Setters
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public Fund getFund() { return fund; }
    public void setFund(Fund fund) { this.fund = fund; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public FundMemberRole getRole() { return role; }
    public void setRole(FundMemberRole role) { this.role = role; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
}

