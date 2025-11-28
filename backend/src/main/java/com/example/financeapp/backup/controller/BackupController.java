package com.example.financeapp.backup.controller;

import com.example.financeapp.backup.dto.BackupHistoryResponse;
import com.example.financeapp.backup.service.BackupService;
import com.example.financeapp.cloud.storage.CloudStorageService;
import com.example.financeapp.security.CustomUserDetails;
import com.example.financeapp.user.entity.User;
import com.example.financeapp.user.repository.UserRepository;
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
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CloudStorageService cloudStorageService;

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

    /**
     * Bật/tắt đồng bộ tự động (auto backup)
     */
    @PutMapping("/auto-sync")
    public ResponseEntity<Map<String, Object>> toggleAutoSync(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody Map<String, Boolean> request
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            Boolean enabled = request.get("enabled");
            
            if (enabled == null) {
                res.put("error", "Vui lòng cung cấp trường 'enabled' (true/false)");
                return ResponseEntity.badRequest().body(res);
            }
            
            user.setAutoBackupEnabled(enabled);
            user = userRepository.save(user);
            
            res.put("message", enabled ? "Đã bật đồng bộ tự động" : "Đã tắt đồng bộ tự động");
            res.put("autoBackupEnabled", user.isAutoBackupEnabled());
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }
    
    /**
     * Lấy trạng thái đồng bộ tự động
     */
    @GetMapping("/auto-sync")
    public ResponseEntity<Map<String, Object>> getAutoSyncStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            res.put("autoBackupEnabled", user.isAutoBackupEnabled());
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }
    
    /**
     * Kiểm tra trạng thái cấu hình cloud backup
     */
    @GetMapping("/config-status")
    public ResponseEntity<Map<String, Object>> getBackupConfigStatus() {
        Map<String, Object> res = new HashMap<>();
        try {
            // Kiểm tra xem cloud backup có được cấu hình không
            boolean isConfigured = cloudStorageService.isEnabled();
            
            res.put("isConfigured", isConfigured);
            res.put("message", isConfigured 
                    ? "Cloud backup đã được cấu hình" 
                    : "Chức năng sao lưu cloud chưa được cấu hình. Vui lòng liên hệ quản trị viên để được hỗ trợ.");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("isConfigured", false);
            res.put("error", e.getMessage());
            return ResponseEntity.ok(res); // Vẫn trả về 200 để frontend có thể xử lý
        }
    }
}

