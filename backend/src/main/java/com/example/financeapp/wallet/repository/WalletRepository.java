package com.example.financeapp.repository;

import com.example.financeapp.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional; // <-- Cần thiết cho việc tìm kiếm chi tiết 1 đối tượng

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    List<Wallet> findByUser_UserId(Long userId);
    boolean existsByWalletNameAndUser_UserId(String walletName, Long userId);

    Wallet findByWalletNameAndUser_UserId(String walletName, Long userId);

    Optional<Wallet> findByWalletIdAndUser_UserId(Long walletId, Long userId);

    @Modifying
    @Query("UPDATE Wallet w SET w.isDefault = FALSE WHERE w.user.userId = :userId AND (:walletId IS NULL OR w.walletId != :walletId)")
    void unsetDefaultWallet(@Param("userId") Long userId, @Param("walletId") Long walletId);

    @Modifying
    @Query("UPDATE Wallet w SET w.isDefault = TRUE WHERE w.walletId = :walletId AND w.user.userId = :userId")
    void setDefaultWallet(@Param("userId") Long userId, @Param("walletId") Long walletId);

    // ✅ Lấy wallet với PESSIMISTIC LOCK để tránh race condition khi transfer/transaction
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.walletId = :walletId")
    Optional<Wallet> findByIdWithLock(@Param("walletId") Long walletId);
}