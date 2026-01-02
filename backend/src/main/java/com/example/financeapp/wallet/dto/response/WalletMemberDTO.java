package com.example.financeapp.wallet.dto.response;

import java.time.LocalDateTime;

/**
 * DTO để trả về thông tin member của wallet
 */
public class WalletMemberDTO {
    private Long memberId;
    private Long userId;
    private String fullName;
    private String email;
    private String avatar;
    private String role; // "OWNER" hoặc "MEMBER"
    private LocalDateTime joinedAt;

    // Constructors
    public WalletMemberDTO() {
    }

    public WalletMemberDTO(Long memberId, Long userId, String fullName, String email,
                           String avatar, String role, LocalDateTime joinedAt) {
        this.memberId = memberId;
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.avatar = avatar;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    // Getters & Setters
    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
}

