package com.example.financeapp.controller;

import com.example.financeapp.dto.CreateWalletRequest;
import com.example.financeapp.entity.User; // <-- Thêm
import com.example.financeapp.entity.Wallet;
import com.example.financeapp.repository.UserRepository; // <-- Thêm
import com.example.financeapp.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional; // <-- Thêm

@RestController
@RequestMapping("/wallets")
@CrossOrigin(origins = "*")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserRepository userRepository; // <-- Thêm

    // (Hàm getCurrentUserId() không còn cần thiết và đã bị xoá)

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        Map<String, Object> res = new HashMap<>();
        try {
            // Lấy email từ token (của user đã đăng nhập)
            String email = SecurityContextHolder.getContext().getAuthentication().getName();

            // Dùng email tìm User trong database
            Optional<User> userOpt = userRepository.findByEmail(email);

            if (userOpt.isEmpty()) {
                res.put("error", "Không tìm thấy user với email: " + email);
                return ResponseEntity.status(401).body(res); // 401 Unauthorized
            }

            // Lấy userId THẬT của người đã đăng nhập
            Long userId = userOpt.get().getUserId();

            // Gọi service với userId thật
            Wallet wallet = walletService.createWallet(userId, request);

            res.put("message", "Tạo ví thành công");
            res.put("wallet", wallet);
            return ResponseEntity.ok(res);

        } catch (RuntimeException e) { // Bắt lỗi nghiệp vụ (vd: trùng tên ví)
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) { // Bắt lỗi chung
            res.put("error", "Lỗi máy chủ nội bộ: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    @GetMapping
    public ResponseEntity<?> getMyWallets() {
        // Sửa tương tự cho hàm GET
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            Map<String, Object> res = new HashMap<>();
            res.put("error", "Không tìm thấy thông tin user từ token");
            return ResponseEntity.status(401).body(res);
        }

        Long userId = userOpt.get().getUserId();
        return ResponseEntity.ok(walletService.getWalletsByUserId(userId));
    }
}