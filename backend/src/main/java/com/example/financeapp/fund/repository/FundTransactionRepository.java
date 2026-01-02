package com.example.financeapp.fund.repository;

import com.example.financeapp.fund.entity.FundTransaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FundTransactionRepository extends JpaRepository<FundTransaction, Long> {

    @Query("""
        SELECT tx FROM FundTransaction tx
        JOIN FETCH tx.fund f
        LEFT JOIN FETCH tx.performedBy u
        WHERE f.fundId = :fundId
          AND tx.status = com.example.financeapp.fund.entity.FundTransactionStatus.SUCCESS
        ORDER BY tx.createdAt DESC
        """)
    List<FundTransaction> findByFundId(@Param("fundId") Long fundId, Pageable pageable);

    /**
     * Tìm các giao dịch nạp tiền hôm nay (manual deposit) của quỹ
     */
    @Query("""
        SELECT tx FROM FundTransaction tx
        WHERE tx.fund.fundId = :fundId
          AND tx.type = com.example.financeapp.fund.entity.FundTransactionType.DEPOSIT
          AND tx.status = com.example.financeapp.fund.entity.FundTransactionStatus.SUCCESS
          AND DATE(tx.createdAt) = CURRENT_DATE
        """)
    List<FundTransaction> findTodayManualDeposits(@Param("fundId") Long fundId);

    @Query("""
        SELECT tx FROM FundTransaction tx
        JOIN FETCH tx.fund f
        LEFT JOIN FETCH tx.performedBy u
        WHERE (f.sourceWallet.walletId = :walletId OR f.targetWallet.walletId = :walletId)
          AND tx.status = com.example.financeapp.fund.entity.FundTransactionStatus.SUCCESS
        ORDER BY tx.createdAt DESC
        """)
    List<FundTransaction> findByWalletId(@Param("walletId") Long walletId);

    @Query("""
        SELECT tx FROM FundTransaction tx
        JOIN FETCH tx.fund f
        LEFT JOIN FETCH tx.performedBy u
        WHERE f.owner.userId = :userId
          AND tx.status = com.example.financeapp.fund.entity.FundTransactionStatus.SUCCESS
        ORDER BY tx.createdAt DESC
        """)
    List<FundTransaction> findByUserId(@Param("userId") Long userId);
}

