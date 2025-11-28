package com.example.financeapp.backup.service;

import com.example.financeapp.backup.dto.BackupHistoryResponse;

import java.util.List;

public interface BackupService {

    /**
     * Thực hiện backup dữ liệu của user
     */
    BackupHistoryResponse triggerBackup(Long userId);

    /**
     * Lấy lịch sử backup của user
     */
    List<BackupHistoryResponse> getBackupHistory(Long userId);
}

