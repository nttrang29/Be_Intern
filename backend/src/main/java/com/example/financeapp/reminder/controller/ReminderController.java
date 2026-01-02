package com.example.financeapp.reminder.controller;

import com.example.financeapp.reminder.dto.ReminderSettingsRequest;
import com.example.financeapp.reminder.dto.ReminderSettingsResponse;
import com.example.financeapp.reminder.service.ReminderService;
import com.example.financeapp.security.CustomUserDetails;
import com.example.financeapp.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/reminders")
@CrossOrigin(origins = "*")
public class ReminderController {

    @Autowired
    private ReminderService reminderService;

    /**
     * Lấy cấu hình nhắc nhở của user hiện tại
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getReminderSettings(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            ReminderSettingsResponse settings = reminderService.getReminderSettings(user.getUserId());
            
            res.put("reminder", settings);
            return ResponseEntity.ok(res);
            
        } catch (Exception e) {
            res.put("error", e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Cập nhật cấu hình nhắc nhở
     */
    @PutMapping
    public ResponseEntity<Map<String, Object>> updateReminderSettings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ReminderSettingsRequest request
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            ReminderSettingsResponse settings = reminderService.updateReminderSettings(
                    user.getUserId(), request);
            
            res.put("message", "Cập nhật cấu hình nhắc nhở thành công");
            res.put("reminder", settings);
            return ResponseEntity.ok(res);
            
        } catch (Exception e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }
}

