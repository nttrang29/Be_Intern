package com.example.financeapp.wallet.entity;

import com.example.financeapp.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "wallet_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"wallet_id", "user_id"})
)
public class WalletMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @ManyToOne
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private WalletRole role;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt = LocalDateTime.now();

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Enum cho roles
    public enum WalletRole {
        OWNER,   // Chủ sở hữu ví
        MEMBER,  // Thành viên được chia sẻ
        VIEW     // Chỉ quyền xem
    }

    // Constructors
    public WalletMember() {
    }

    public WalletMember(Wallet wallet, User user, WalletRole role) {
        this.wallet = wallet;
        this.user = user;
        this.role = role;
        this.joinedAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public WalletRole getRole() {
        return role;
    }

    public void setRole(WalletRole role) {
        this.role = role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public boolean isDeleted() {
        return deleted != null && deleted;
    }

    // Helper methods
    public boolean isOwner() {
        return this.role == WalletRole.OWNER;
    }

    public boolean isMember() {
        return this.role == WalletRole.MEMBER;
    }
}

