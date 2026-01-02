package com.example.financeapp.backup.scheduler;

import com.example.financeapp.backup.service.BackupService;
import com.example.financeapp.cloud.storage.CloudStorageService;
import com.example.financeapp.user.entity.User;
import com.example.financeapp.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BackupScheduler {

    private static final Logger log = LoggerFactory.getLogger(BackupScheduler.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BackupService backupService;

    @Autowired
    private CloudStorageService cloudStorageService;

    /**
     * Chạy mỗi ngày lúc 02:00 sáng
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void runDailyBackup() {
        if (!cloudStorageService.isEnabled()) {
            log.debug("Cloud backup disabled -> skip scheduled task");
            return;
        }

        // Chỉ backup user có bật auto backup
        List<User> users = userRepository.findByEnabledTrueAndDeletedFalseAndAutoBackupEnabledTrue();
        log.info("Bắt đầu backup tự động cho {} user (đã bật auto backup)", users.size());

        for (User user : users) {
            try {
                backupService.triggerBackup(user.getUserId());
            } catch (Exception e) {
                log.error("Backup tự động thất bại cho user {}: {}", user.getEmail(), e.getMessage());
            }
        }
    }
}

