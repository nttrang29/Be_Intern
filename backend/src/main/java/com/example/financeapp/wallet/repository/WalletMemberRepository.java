package com.example.financeapp.wallet.repository;

import com.example.financeapp.wallet.entity.WalletMember;
import com.example.financeapp.wallet.entity.WalletMember.WalletRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletMemberRepository extends JpaRepository<WalletMember, Long> {

    // Tìm tất cả members của một wallet (chưa bị xóa mềm)
    @Query("SELECT wm FROM WalletMember wm WHERE wm.wallet.walletId = :walletId AND (wm.deleted IS NULL OR wm.deleted = false)")
    List<WalletMember> findByWallet_WalletId(@Param("walletId") Long walletId);

    // Tìm tất cả wallets mà user là member (bao gồm owner) (chưa bị xóa mềm)
    @Query("SELECT wm FROM WalletMember wm WHERE wm.user.userId = :userId AND (wm.deleted IS NULL OR wm.deleted = false)")
    List<WalletMember> findByUser_UserId(@Param("userId") Long userId);

    // Tìm tất cả wallets mà user là owner (chưa bị xóa mềm)
    @Query("SELECT wm FROM WalletMember wm WHERE wm.user.userId = :userId AND wm.role = :role AND (wm.deleted IS NULL OR wm.deleted = false)")
    List<WalletMember> findByUser_UserIdAndRole(@Param("userId") Long userId, @Param("role") WalletRole role);

    // Kiểm tra user có phải member của wallet không (chưa bị xóa mềm)
    @Query("SELECT CASE WHEN COUNT(wm) > 0 THEN true ELSE false END " +
            "FROM WalletMember wm " +
            "WHERE wm.wallet.walletId = :walletId " +
            "AND wm.user.userId = :userId " +
            "AND (wm.deleted IS NULL OR wm.deleted = false)")
    boolean existsByWallet_WalletIdAndUser_UserId(@Param("walletId") Long walletId, @Param("userId") Long userId);

    // Tìm member cụ thể trong wallet (bao gồm cả đã bị xóa mềm - để có thể kiểm tra và xử lý)
    // Query này không filter deleted để có thể tìm thấy member đã bị xóa mềm
    Optional<WalletMember> findByWallet_WalletIdAndUser_UserId(Long walletId, Long userId);

    // Xóa member khỏi wallet
    void deleteByWallet_WalletIdAndUser_UserId(Long walletId, Long userId);

    // Kiểm tra user có phải owner của wallet không (chưa bị xóa mềm)
    @Query("SELECT CASE WHEN COUNT(wm) > 0 THEN true ELSE false END " +
            "FROM WalletMember wm " +
            "WHERE wm.wallet.walletId = :walletId " +
            "AND wm.user.userId = :userId " +
            "AND wm.role = 'OWNER' " +
            "AND (wm.deleted IS NULL OR wm.deleted = false)")
    boolean isOwner(@Param("walletId") Long walletId, @Param("userId") Long userId);

    // Đếm số lượng members trong wallet (chưa bị xóa mềm)
    @Query("SELECT COUNT(wm) FROM WalletMember wm WHERE wm.wallet.walletId = :walletId AND (wm.deleted IS NULL OR wm.deleted = false)")
    long countByWallet_WalletId(@Param("walletId") Long walletId);

    // Lấy owner của wallet (chưa bị xóa mềm)
    @Query("SELECT wm FROM WalletMember wm WHERE wm.wallet.walletId = :walletId AND wm.role = :role AND (wm.deleted IS NULL OR wm.deleted = false)")
    Optional<WalletMember> findByWallet_WalletIdAndRole(@Param("walletId") Long walletId, @Param("role") WalletRole role);
}

