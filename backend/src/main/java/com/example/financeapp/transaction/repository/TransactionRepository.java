package com.example.financeapp.transaction.repository;

import com.example.financeapp.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Lấy giao dịch theo user
    List<Transaction> findByUser_UserIdOrderByTransactionDateDesc(Long userId);

    @Query(value = "SELECT * FROM transactions WHERE user_id = :userId ORDER BY transaction_date DESC", nativeQuery = true)
    List<Transaction> findAllByUser_UserIdOrderByTransactionDateDescIncludingDeleted(@Param("userId") Long userId);

    // Kiểm tra wallet có giao dịch không
    boolean existsByWallet_WalletId(Long walletId);

    // ===== MERGE WALLET METHODS =====

    /**
     * Lấy tất cả transactions của một wallet
     */
    List<Transaction> findByWallet_WalletId(Long walletId);

    @Query("""
                SELECT DISTINCT t
                FROM Transaction t
                LEFT JOIN FETCH t.user
                LEFT JOIN FETCH t.wallet
                LEFT JOIN FETCH t.category
                LEFT JOIN FETCH t.transactionType
                WHERE t.wallet.walletId = :walletId
                ORDER BY t.transactionDate DESC
                """)
    List<Transaction> findDetailedByWalletId(@Param("walletId") Long walletId);

    @Query(value = "SELECT * FROM transactions WHERE wallet_id = :walletId ORDER BY transaction_date DESC", nativeQuery = true)
    List<Transaction> findDetailedByWalletIdIncludingDeleted(@Param("walletId") Long walletId);

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

    /**
     * Tính tổng số tiền đã chi (EXPENSE) trong budget
     * - Theo user, category, wallet (hoặc tất cả ví nếu walletId = null)
     * - Trong khoảng thời gian từ startDate đến endDate
     */
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.user.userId = :userId
          AND t.category.categoryId = :categoryId
          AND t.transactionType.typeName = 'Chi tiêu'
          AND (:walletId IS NULL OR t.wallet.walletId = :walletId)
          AND DATE(t.transactionDate) >= :startDate
          AND DATE(t.transactionDate) <= :endDate
        """)
    BigDecimal calculateTotalSpent(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("walletId") Long walletId,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate
    );

    /**
     * Lấy danh sách giao dịch (EXPENSE) thuộc một budget
     * - Theo user, category, wallet (hoặc tất cả ví nếu walletId = null)
     * - Trong khoảng thời gian từ startDate đến endDate
     * - Sắp xếp theo ngày giao dịch giảm dần (mới nhất trước)
     */
    @Query("""
        SELECT t
        FROM Transaction t
        WHERE t.user.userId = :userId
          AND t.category.categoryId = :categoryId
          AND t.transactionType.typeName = 'Chi tiêu'
          AND (:walletId IS NULL OR t.wallet.walletId = :walletId)
          AND DATE(t.transactionDate) >= :startDate
          AND DATE(t.transactionDate) <= :endDate
        ORDER BY t.transactionDate DESC
        """)
    List<Transaction> findTransactionsByBudget(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("walletId") Long walletId,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate
    );

    /**
     * Kiểm tra user có giao dịch trong khoảng thời gian không
     */
    boolean existsByUser_UserIdAndTransactionDateBetween(
            Long userId,
            java.time.LocalDateTime start,
            java.time.LocalDateTime end
    );

    /**
     * Lấy ngày giao dịch sớm nhất trong phạm vi budget để phục vụ việc chỉnh sửa.
     */
    @Query("""
        SELECT MIN(DATE(t.transactionDate))
        FROM Transaction t
        WHERE t.user.userId = :userId
          AND t.category.categoryId = :categoryId
          AND (:walletId IS NULL OR t.wallet.walletId = :walletId)
        """)
    java.time.LocalDate findEarliestTransactionDate(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("walletId") Long walletId
    );
}
