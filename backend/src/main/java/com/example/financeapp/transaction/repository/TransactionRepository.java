package com.example.financeapp.repository;

import com.example.financeapp.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Lấy giao dịch theo user
    List<Transaction> findByUser_UserIdOrderByTransactionDateDesc(Long userId);

    // Kiểm tra wallet có giao dịch không
    boolean existsByWallet_WalletId(Long walletId);

    // ===== MERGE WALLET METHODS =====

    /**
     * Lấy tất cả transactions của một wallet
     */
    List<Transaction> findByWallet_WalletId(Long walletId);

    /**
     * Đếm số lượng transactions trong wallet
     */
    long countByWallet_WalletId(Long walletId);

    /**
     * Update wallet_id cho tất cả transactions (khi merge)
     * Chuyển tất cả transactions từ sourceWalletId sang targetWalletId
     */
    @Modifying
    @Query("UPDATE Transaction t SET t.wallet.walletId = :targetWalletId " +
            "WHERE t.wallet.walletId = :sourceWalletId")
    int updateWalletIdForAllTransactions(
            @Param("sourceWalletId") Long sourceWalletId,
            @Param("targetWalletId") Long targetWalletId
    );

    /**
     * Kiểm tra category có trong giao dịch không
     */
    boolean existsByCategory_CategoryId(Long categoryId);
}
