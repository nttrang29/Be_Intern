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
          AND (f.deleted IS NULL OR f.deleted = false)
        ORDER BY tx.createdAt DESC
        """)
    List<FundTransaction> findByFundId(@Param("fundId") Long fundId, Pageable pageable);
}

