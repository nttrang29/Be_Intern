package com.example.financeapp.backup.repository;

import com.example.financeapp.backup.entity.BackupHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BackupHistoryRepository extends JpaRepository<BackupHistory, Long> {
    List<BackupHistory> findByUser_UserIdOrderByRequestedAtDesc(Long userId);
}

