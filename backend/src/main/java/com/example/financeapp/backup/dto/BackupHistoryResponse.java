package com.example.financeapp.backup.dto;

import com.example.financeapp.backup.entity.BackupHistory;
import com.example.financeapp.backup.entity.BackupStatus;

import java.time.LocalDateTime;

public class BackupHistoryResponse {
    private Long backupId;
    private BackupStatus status;
    private String fileUrl;
    private Long fileSizeBytes;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    private String errorMessage;

    public static BackupHistoryResponse fromEntity(BackupHistory history) {
        BackupHistoryResponse response = new BackupHistoryResponse();
        response.setBackupId(history.getBackupId());
        response.setStatus(history.getStatus());
        response.setFileUrl(history.getFileUrl());
        response.setFileSizeBytes(history.getFileSizeBytes());
        response.setRequestedAt(history.getRequestedAt());
        response.setCompletedAt(history.getCompletedAt());
        response.setErrorMessage(history.getErrorMessage());
        return response;
    }

    public Long getBackupId() {
        return backupId;
    }

    public void setBackupId(Long backupId) {
        this.backupId = backupId;
    }

    public BackupStatus getStatus() {
        return status;
    }

    public void setStatus(BackupStatus status) {
        this.status = status;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

