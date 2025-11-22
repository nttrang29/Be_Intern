package com.example.financeapp.wallet.repository;

import com.example.financeapp.wallet.entity.WalletTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WalletTransferRepository extends JpaRepository<WalletTransfer, Long> {

    /**
     * Lấy tất cả transfers của user (theo thời gian giảm dần)
     * Sử dụng JOIN FETCH để load các relationships và tránh lazy loading exception
     */
    @Query("SELECT t FROM WalletTransfer t " +
            "LEFT JOIN FETCH t.fromWallet " +
            "LEFT JOIN FETCH t.toWallet " +
            "LEFT JOIN FETCH t.user " +
            "WHERE t.user.userId = :userId " +
            "ORDER BY t.transferDate DESC")
    List<WalletTransfer> findByUser_UserIdOrderByTransferDateDesc(@Param("userId") Long userId);

    /**
     * Lấy transfers của một ví cụ thể (cả gửi và nhận)
     * Sử dụng JOIN FETCH để load relationships và tránh lazy loading exception
     */
    @Query("SELECT t FROM WalletTransfer t " +
            "LEFT JOIN FETCH t.fromWallet " +
            "LEFT JOIN FETCH t.toWallet " +
            "LEFT JOIN FETCH t.user " +
            "WHERE t.fromWallet.walletId = :walletId OR t.toWallet.walletId = :walletId " +
            "ORDER BY t.transferDate DESC")
    List<WalletTransfer> findByWalletId(@Param("walletId") Long walletId);

    /**
     * Lấy transfers từ một ví cụ thể (chỉ gửi đi)
     */
    List<WalletTransfer> findByFromWallet_WalletIdOrderByTransferDateDesc(Long walletId);

    /**
     * Lấy transfers đến một ví cụ thể (chỉ nhận vào)
     */
    List<WalletTransfer> findByToWallet_WalletIdOrderByTransferDateDesc(Long walletId);

    /**
     * Lấy transfers trong khoảng thời gian
     */
    @Query("SELECT t FROM WalletTransfer t " +
            "WHERE t.user.userId = :userId " +
            "AND t.transferDate BETWEEN :startDate AND :endDate " +
            "ORDER BY t.transferDate DESC")
    List<WalletTransfer> findByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Đếm số lần transfer giữa 2 ví
     */
    @Query("SELECT COUNT(t) FROM WalletTransfer t " +
            "WHERE (t.fromWallet.walletId = :wallet1Id AND t.toWallet.walletId = :wallet2Id) " +
            "OR (t.fromWallet.walletId = :wallet2Id AND t.toWallet.walletId = :wallet1Id)")
    long countTransfersBetweenWallets(
            @Param("wallet1Id") Long wallet1Id,
            @Param("wallet2Id") Long wallet2Id
    );

    /**
     * Xóa tất cả transfers liên quan đến một ví (khi xóa ví)
     */
    void deleteByFromWallet_WalletIdOrToWallet_WalletId(Long fromWalletId, Long toWalletId);

    /**
     * Đếm số transfers của user
     */
    long countByUser_UserId(Long userId);

    /**
     * Lấy transfer với tất cả relationships được fetch (để tránh lazy loading exception và serialization issues)
     */
    @Query("SELECT t FROM WalletTransfer t " +
            "LEFT JOIN FETCH t.user " +
            "LEFT JOIN FETCH t.fromWallet " +
            "LEFT JOIN FETCH t.toWallet " +
            "WHERE t.transferId = :transferId")
    Optional<WalletTransfer> findByIdWithUser(@Param("transferId") Long transferId);

    /**
     * Lấy transfer với tất cả relationships để xóa (cần fetch để revert balance)
     */
    @Query("SELECT t FROM WalletTransfer t " +
            "LEFT JOIN FETCH t.user " +
            "LEFT JOIN FETCH t.fromWallet " +
            "LEFT JOIN FETCH t.toWallet " +
            "WHERE t.transferId = :transferId")
    Optional<WalletTransfer> findByIdForDelete(@Param("transferId") Long transferId);
}

