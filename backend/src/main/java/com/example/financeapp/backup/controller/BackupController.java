package com.example.financeapp.backup.controller;

import com.example.financeapp.backup.dto.BackupHistoryResponse;
import com.example.financeapp.backup.service.BackupService;
import com.example.financeapp.security.CustomUserDetails;
import com.example.financeapp.user.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/backups")
@CrossOrigin(origins = "*")
public class BackupController {

    @Autowired
    private BackupService backupService;

    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerBackup(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            BackupHistoryResponse backup = backupService.triggerBackup(user.getUserId());
            res.put("message", "Backup dữ liệu thành công");
            res.put("backup", backup);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            List<BackupHistoryResponse> history = backupService.getBackupHistory(user.getUserId());
            res.put("history", history);
            res.put("total", history.size());
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }
}

