package com.example.financeapp.fund.dto;

import com.example.financeapp.fund.entity.FundMember;
import java.time.LocalDateTime;

/**
 * DTO response cho thành viên quỹ
 */
public class FundMemberResponse {
    private Long memberId;
    private Long userId;
    private String userName;
    private String userEmail;
    private String role;
    private LocalDateTime joinedAt;

    public static FundMemberResponse fromEntity(FundMember member) {
        FundMemberResponse response = new FundMemberResponse();
        response.setMemberId(member.getMemberId());
        response.setUserId(member.getUser().getUserId());
        response.setUserName(member.getUser().getFullName());
        response.setUserEmail(member.getUser().getEmail());
        response.setRole(member.getRole().name());
        response.setJoinedAt(member.getJoinedAt());
        return response;
    }

    // Getters & Setters
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
}

