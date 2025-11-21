package com.example.financeapp.repository;

import com.example.financeapp.entity.WalletMergeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletMergeHistoryRepository extends JpaRepository<WalletMergeHistory, Long> {

    /**
     * Lấy lịch sử merge của user (mới nhất trước)
     */
    List<WalletMergeHistory> findByUserIdOrderByMergedAtDesc(Long userId);

    /**
     * Đếm số lần user đã merge wallet
     */
    long countByUserId(Long userId);
}

