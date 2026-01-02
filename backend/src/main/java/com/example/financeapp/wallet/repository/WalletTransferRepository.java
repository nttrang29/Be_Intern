package com.example.financeapp.wallet.repository;

import com.example.financeapp.wallet.entity.WalletTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
    @Query("SELECT DISTINCT t FROM WalletTransfer t " +
            "LEFT JOIN FETCH t.fromWallet " +
            "LEFT JOIN FETCH t.toWallet " +
            "LEFT JOIN FETCH t.user " +
            "WHERE t.user.userId = :userId " +
            "AND t.status = com.example.financeapp.wallet.entity.WalletTransfer.TransferStatus.COMPLETED " +
            "ORDER BY t.transferDate DESC")
    List<WalletTransfer> findByUser_UserIdOrderByTransferDateDesc(@Param("userId") Long userId);

    @Query(value = "SELECT * FROM wallet_transfers WHERE user_id = :userId AND status = 'COMPLETED' ORDER BY transfer_date DESC", nativeQuery = true)
    List<WalletTransfer> findAllByUser_UserIdOrderByTransferDateDescIncludingDeleted(@Param("userId") Long userId);

    /**
     * Lấy transfers của một ví cụ thể (cả gửi và nhận)
     * Sử dụng JOIN FETCH để load relationships và tránh lazy loading exception
     */
    @Query("SELECT DISTINCT t FROM WalletTransfer t " +
            "LEFT JOIN FETCH t.fromWallet " +
            "LEFT JOIN FETCH t.toWallet " +
            "LEFT JOIN FETCH t.user " +
            "WHERE (t.fromWallet.walletId = :walletId OR t.toWallet.walletId = :walletId) " +
            "AND t.status = com.example.financeapp.wallet.entity.WalletTransfer.TransferStatus.COMPLETED " +
            "ORDER BY t.transferDate DESC")
    List<WalletTransfer> findByWalletId(@Param("walletId") Long walletId);

    @Query(value = "SELECT * FROM wallet_transfers WHERE (from_wallet_id = :walletId OR to_wallet_id = :walletId) AND status = 'COMPLETED' ORDER BY transfer_date DESC", nativeQuery = true)
    List<WalletTransfer> findByWalletIdIncludingDeleted(@Param("walletId") Long walletId);

    /**
     * Lấy transfers từ một ví cụ thể (chỉ gửi đi)
     */
    List<WalletTransfer> findByFromWallet_WalletIdOrderByTransferDateDesc(Long walletId);

    /**
     * Lấy transfers đến một ví cụ thể (chỉ nhận vào)
     */
    List<WalletTransfer> findByToWallet_WalletIdOrderByTransferDateDesc(Long walletId);

    /**
     * Lấy tất cả transfers của user (theo thời gian giảm dần), bao gồm cả đã xóa (Native Query)
     */
    @Query(value = "SELECT * FROM wallet_transfers t " +
            "WHERE t.user_id = :userId " +
            "AND t.status = 'COMPLETED' " +
            "ORDER BY t.transfer_date DESC", nativeQuery = true)
    List<WalletTransfer> findByUser_UserIdOrderByTransferDateDescIncludingDeleted(@Param("userId") Long userId);

    /**
     * Lấy transfers của một ví cụ thể (cả gửi và nhận), bao gồm cả đã xóa (Native Query)
     */
    @Query(value = "SELECT * FROM wallet_transfers t " +
            "WHERE (t.from_wallet_id = :walletId OR t.to_wallet_id = :walletId) " +
            "AND t.status = 'COMPLETED' " +
            "ORDER BY t.transfer_date DESC", nativeQuery = true)
    List<WalletTransfer> findDetailedByWalletIdIncludingDeleted(@Param("walletId") Long walletId);

    /**
     * Lấy transfers trong khoảng thời gian
     */
    @Query("SELECT t FROM WalletTransfer t " +
            "WHERE t.user.userId = :userId " +
            "AND t.transferDate BETWEEN :startDate AND :endDate " +
            "AND t.status = com.example.financeapp.wallet.entity.WalletTransfer.TransferStatus.COMPLETED " +
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

    /**
     * Soft delete transfer bằng native query để tránh vấn đề với @Where clause
     */
    @Modifying
    @Query(value = "UPDATE wallet_transfers SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE transfer_id = :transferId", nativeQuery = true)
    void softDelete(@Param("transferId") Long transferId);

    /**
     * Lấy user_id của transfer (kể cả đã xóa) để kiểm tra quyền sở hữu khi transfer đã bị soft delete
     */
    @Query(value = "SELECT user_id FROM wallet_transfers WHERE transfer_id = :transferId", nativeQuery = true)
    Long getUserIdByTransferIdIncludingDeleted(@Param("transferId") Long transferId);
}

