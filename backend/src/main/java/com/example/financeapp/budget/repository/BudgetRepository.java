package com.example.financeapp.budget.repository;

import com.example.financeapp.budget.entity.Budget;
import com.example.financeapp.category.entity.Category;
import com.example.financeapp.user.entity.User;
import com.example.financeapp.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    /**
     * Lấy danh sách budgets của user, sắp xếp theo thời gian tạo mới nhất
     */
    List<Budget> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    // ← METHOD MỚI: kiểm tra trùng chính xác 100%
    @Query("""
        SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
        FROM Budget b
        WHERE b.user = :user
          AND b.category = :category
          AND (
                (b.wallet IS NULL AND :wallet IS NULL) 
             OR (b.wallet IS NOT NULL AND :wallet IS NOT NULL AND b.wallet.walletId = :walletId)
              )
          AND b.startDate = :startDate
          AND b.endDate = :endDate
        """)
    boolean existsExactlySameBudget(
            @Param("user") User user,
            @Param("category") Category category,
            @Param("wallet") Wallet wallet,
            @Param("walletId") Long walletId,   // để xử lý null an toàn
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
    SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
    FROM Budget b
    WHERE b.user = :user
      AND b.category.categoryId = :categoryId
      AND (
            (b.wallet IS NULL AND :walletId IS NULL)
         OR (b.wallet IS NOT NULL AND :walletId IS NOT NULL AND b.wallet.walletId = :walletId)
         OR (b.wallet IS NULL AND :walletId IS NOT NULL)
         OR (b.wallet IS NOT NULL AND :walletId IS NULL)
          )
      AND b.startDate <= :newEndDate
      AND b.endDate >= :newStartDate
    """)
    boolean existsOverlappingBudget(
            @Param("user") User user,
            @Param("categoryId") Long categoryId,
            @Param("walletId") Long walletId,
            @Param("newStartDate") LocalDate newStartDate,
            @Param("newEndDate") LocalDate newEndDate
    );

    /**
     * Tìm các budget áp dụng cho một giao dịch
     * - Cùng user, category
     * - Wallet khớp hoặc budget áp dụng cho tất cả ví (wallet = null)
     * - Transaction date nằm trong khoảng startDate và endDate
     */
    @Query("""
        SELECT b
        FROM Budget b
        WHERE b.user.userId = :userId
          AND b.category.categoryId = :categoryId
          AND (
                b.wallet IS NULL
             OR b.wallet.walletId = :walletId
              )
      AND b.startDate <= :transactionDate
      AND b.endDate >= :transactionDate
        ORDER BY b.createdAt DESC
        """)
    List<Budget> findApplicableBudgets(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("walletId") Long walletId,
            @Param("transactionDate") LocalDate transactionDate
    );

    @Query("""
        SELECT b
        FROM Budget b
        WHERE b.user = :user
          AND b.category.categoryId = :categoryId
          AND (
                (b.wallet IS NULL AND :walletId IS NULL)
             OR (b.wallet IS NOT NULL AND :walletId IS NOT NULL AND b.wallet.walletId = :walletId)
             OR (b.wallet IS NULL AND :walletId IS NOT NULL)
             OR (b.wallet IS NOT NULL AND :walletId IS NULL)
              )
          AND b.startDate <= :newEndDate
          AND b.endDate >= :newStartDate
        """)
    List<Budget> findOverlappingBudgets(
            @Param("user") User user,
            @Param("categoryId") Long categoryId,
            @Param("walletId") Long walletId,
            @Param("newStartDate") LocalDate newStartDate,
            @Param("newEndDate") LocalDate newEndDate
    );
}