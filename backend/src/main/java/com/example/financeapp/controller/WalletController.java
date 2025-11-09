
package com.example.financeapp.controller;

import com.example.financeapp.dto.CreateWalletRequest;
import com.example.financeapp.entity.Wallet;
import com.example.financeapp.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wallets")
@CrossOrigin(origins = "*")
public class WalletController {

    @Autowired
    private WalletService walletService;

    private Long getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return walletService.getWalletsByUserId(0L).isEmpty() ? 1L : 1L; // TODO: Lấy từ UserService
        // Thực tế: dùng UserService.findByEmail(email).getUserId()
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        Map<String, Object> res = new HashMap<>();
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            // TODO: Lấy userId từ email (tạo UserService nếu cần)
            Long userId = 1L; // Mock tạm

            Wallet wallet = walletService.createWallet(userId, request);
            res.put("message", "Tạo ví thành công");
            res.put("wallet", wallet);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    @GetMapping
    public ResponseEntity<List<Wallet>> getMyWallets() {
        Long userId = 1L; // TODO: từ JWT
        return ResponseEntity.ok(walletService.getWalletsByUserId(userId));
    }
}