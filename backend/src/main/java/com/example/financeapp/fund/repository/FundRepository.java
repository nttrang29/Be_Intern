package com.example.financeapp.fund.repository;

import com.example.financeapp.fund.entity.Fund;
import com.example.financeapp.fund.entity.FundStatus;
import com.example.financeapp.fund.entity.FundType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FundRepository extends JpaRepository<Fund, Long> {

    /**
     * Lấy tất cả quỹ của user (cả quỹ cá nhân và quỹ nhóm mà user tham gia)
     */
    @Query("""
        SELECT DISTINCT f FROM Fund f
        LEFT JOIN FETCH f.owner
        LEFT JOIN FETCH f.targetWallet
        LEFT JOIN FETCH f.sourceWallet
        LEFT JOIN FundMember fm ON fm.fund.fundId = f.fundId
        WHERE f.owner.userId = :userId OR fm.user.userId = :userId
        ORDER BY f.createdAt DESC
        """)
    List<Fund> findByUserInvolved(@Param("userId") Long userId);

    /**
     * Lấy quỹ cá nhân của user
     */
    @Query("""
        SELECT DISTINCT f FROM Fund f
        LEFT JOIN FETCH f.owner
        LEFT JOIN FETCH f.targetWallet
        LEFT JOIN FETCH f.sourceWallet
        WHERE f.owner.userId = :userId AND f.fundType = :fundType
        ORDER BY f.createdAt DESC
        """)
    List<Fund> findByOwner_UserIdAndFundTypeOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("fundType") FundType fundType
    );

    /**
     * Lấy quỹ nhóm mà user là chủ quỹ
     */
    @Query("""
        SELECT DISTINCT f FROM Fund f
        LEFT JOIN FETCH f.owner
        LEFT JOIN FETCH f.targetWallet
        LEFT JOIN FETCH f.sourceWallet
        WHERE f.owner.userId = :userId 
          AND f.fundType = :fundType 
          AND f.status = :status
        ORDER BY f.createdAt DESC
        """)
    List<Fund> findByOwner_UserIdAndFundTypeAndStatusOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("fundType") FundType fundType,
            @Param("status") FundStatus status
    );

    /**
     * Kiểm tra ví đã được sử dụng cho quỹ hoặc ngân sách chưa
     */
    @Query("""
        SELECT COUNT(f) > 0 FROM Fund f
        WHERE f.targetWallet.walletId = :walletId
        """)
    boolean existsByTargetWallet_WalletId(@Param("walletId") Long walletId);

    /**
     * Lấy quỹ theo status
     */
    @Query("""
        SELECT DISTINCT f FROM Fund f
        LEFT JOIN FETCH f.owner
        LEFT JOIN FETCH f.targetWallet
        LEFT JOIN FETCH f.sourceWallet
        WHERE f.status = :status
        ORDER BY f.createdAt DESC
        """)
    List<Fund> findByStatusOrderByCreatedAtDesc(@Param("status") FundStatus status);

    /**
     * Lấy quỹ có deadline và đang active
     */
    @Query("""
        SELECT DISTINCT f FROM Fund f
        LEFT JOIN FETCH f.owner
        LEFT JOIN FETCH f.targetWallet
        LEFT JOIN FETCH f.sourceWallet
        WHERE f.hasDeadline = true
          AND f.status = 'ACTIVE'
          AND f.endDate <= :today
        """)
    List<Fund> findExpiredActiveFunds(@Param("today") java.time.LocalDate today);

    /**
     * Lấy quỹ theo ID với eager load các entity liên quan
     */
    @Query("""
        SELECT DISTINCT f FROM Fund f
        LEFT JOIN FETCH f.owner
        LEFT JOIN FETCH f.targetWallet
        LEFT JOIN FETCH f.sourceWallet
        WHERE f.fundId = :fundId
        """)
    java.util.Optional<Fund> findByIdWithRelations(@Param("fundId") Long fundId);

    /**
     * Lấy tất cả quỹ có targetWalletId = walletId
     */
    @Query("""
        SELECT f FROM Fund f
        WHERE f.targetWallet.walletId = :walletId
        """)
    List<Fund> findByTargetWallet_WalletId(@Param("walletId") Long walletId);
}

