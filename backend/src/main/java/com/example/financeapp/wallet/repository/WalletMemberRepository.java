package com.example.financeapp.repository;

import com.example.financeapp.entity.WalletMember;
import com.example.financeapp.entity.WalletMember.WalletRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletMemberRepository extends JpaRepository<WalletMember, Long> {

    // Tìm tất cả members của một wallet
    List<WalletMember> findByWallet_WalletId(Long walletId);

    // Tìm tất cả wallets mà user là member (bao gồm owner)
    List<WalletMember> findByUser_UserId(Long userId);

    // Tìm tất cả wallets mà user là owner
    List<WalletMember> findByUser_UserIdAndRole(Long userId, WalletRole role);

    // Kiểm tra user có phải member của wallet không
    boolean existsByWallet_WalletIdAndUser_UserId(Long walletId, Long userId);

    // Tìm member cụ thể trong wallet
    Optional<WalletMember> findByWallet_WalletIdAndUser_UserId(Long walletId, Long userId);

    // Xóa member khỏi wallet
    void deleteByWallet_WalletIdAndUser_UserId(Long walletId, Long userId);

    // Kiểm tra user có phải owner của wallet không
    @Query("SELECT CASE WHEN COUNT(wm) > 0 THEN true ELSE false END " +
            "FROM WalletMember wm " +
            "WHERE wm.wallet.walletId = :walletId " +
            "AND wm.user.userId = :userId " +
            "AND wm.role = 'OWNER'")
    boolean isOwner(@Param("walletId") Long walletId, @Param("userId") Long userId);

    // Đếm số lượng members trong wallet
    long countByWallet_WalletId(Long walletId);

    // Lấy owner của wallet
    Optional<WalletMember> findByWallet_WalletIdAndRole(Long walletId, WalletRole role);
}

