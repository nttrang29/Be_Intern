package com.example.financeapp.fund.repository;

import com.example.financeapp.fund.entity.Fund;
import com.example.financeapp.fund.entity.FundMember;
import com.example.financeapp.fund.entity.FundMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FundMemberRepository extends JpaRepository<FundMember, Long> {

    /**
     * Lấy tất cả thành viên của quỹ
     */
    @Query("""
        SELECT fm FROM FundMember fm
        LEFT JOIN FETCH fm.user
        WHERE fm.fund.fundId = :fundId
        ORDER BY fm.joinedAt ASC
        """)
    List<FundMember> findByFund_FundIdOrderByJoinedAtAsc(@Param("fundId") Long fundId);

    /**
     * Kiểm tra user có phải thành viên quỹ không
     */
    boolean existsByFund_FundIdAndUser_UserId(Long fundId, Long userId);

    /**
     * Lấy FundMember theo fund và user
     */
    Optional<FundMember> findByFund_FundIdAndUser_UserId(Long fundId, Long userId);

    /**
     * Lấy quỹ nhóm mà user tham gia (không phải chủ quỹ)
     */
    @Query("""
        SELECT DISTINCT fm.fund FROM FundMember fm
        LEFT JOIN FETCH fm.fund.owner
        LEFT JOIN FETCH fm.fund.targetWallet
        LEFT JOIN FETCH fm.fund.sourceWallet
        WHERE fm.user.userId = :userId
          AND (fm.fund.deleted = false OR fm.fund.deleted IS NULL)
          AND fm.role = 'CONTRIBUTOR'
        ORDER BY fm.fund.createdAt DESC
        """)
    List<Fund> findGroupFundsByMember(@Param("userId") Long userId);

    /**
     * Đếm số thành viên của quỹ
     */
    long countByFund_FundId(Long fundId);

    /**
     * Xóa thành viên khỏi quỹ
     */
    void deleteByFund_FundIdAndUser_UserId(Long fundId, Long userId);

    /**
     * Xóa tất cả thành viên của quỹ
     */
    void deleteByFund_FundId(Long fundId);

    /**
     * Cập nhật quyền thành viên
     */
    @Query("""
        UPDATE FundMember fm
        SET fm.role = :role
        WHERE fm.fund.fundId = :fundId AND fm.user.userId = :userId
        """)
    void updateMemberRole(@Param("fundId") Long fundId, @Param("userId") Long userId, @Param("role") FundMemberRole role);
}

