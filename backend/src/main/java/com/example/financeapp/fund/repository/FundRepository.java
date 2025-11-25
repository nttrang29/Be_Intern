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
        LEFT JOIN FundMember fm ON fm.fund.fundId = f.fundId
        WHERE f.owner.userId = :userId OR fm.user.userId = :userId
        ORDER BY f.createdAt DESC
        """)
    List<Fund> findByUserInvolved(@Param("userId") Long userId);

    /**
     * Lấy quỹ cá nhân của user
     */
    List<Fund> findByOwner_UserIdAndFundTypeOrderByCreatedAtDesc(Long userId, FundType fundType);

    /**
     * Lấy quỹ nhóm mà user là chủ quỹ
     */
    List<Fund> findByOwner_UserIdAndFundTypeAndStatusOrderByCreatedAtDesc(
            Long userId, FundType fundType, FundStatus status);

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
    List<Fund> findByStatusOrderByCreatedAtDesc(FundStatus status);

    /**
     * Lấy quỹ có deadline và đang active
     */
    @Query("""
        SELECT f FROM Fund f
        WHERE f.hasDeadline = true
          AND f.status = 'ACTIVE'
          AND f.endDate <= :today
        """)
    List<Fund> findExpiredActiveFunds(@Param("today") java.time.LocalDate today);
}

