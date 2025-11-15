package com.example.financeapp.controller;

import com.example.financeapp.dto.*;
import com.example.financeapp.entity.User;
import com.example.financeapp.entity.Wallet;
import com.example.financeapp.repository.UserRepository;
import com.example.financeapp.service.WalletService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/wallets")
@CrossOrigin(origins = "*")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Helper method để lấy userId từ JWT token
     */
    private Long getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin user từ token"))
                .getUserId();
    }

    // ========================= CREATE WALLET =========================

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        Map<String, Object> res = new HashMap<>();
        try {
            Long userId = getCurrentUserId();
            Wallet wallet = walletService.createWallet(userId, request);

            res.put("message", "Tạo ví thành công");
            res.put("wallet", wallet);
            return ResponseEntity.ok(res);

        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi máy chủ nội bộ: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    // ========================= GET ALL WALLETS =========================

    @GetMapping
    public ResponseEntity<Map<String, Object>> getMyWallets() {
        Map<String, Object> res = new HashMap<>();
        try {
            Long userId = getCurrentUserId();

            List<SharedWalletDTO> wallets = walletService.getAllAccessibleWallets(userId);

            res.put("wallets", wallets);
            res.put("total", wallets.size());
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            res.put("error", e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    // ========================= GET WALLET DETAILS =========================

    @GetMapping("/{walletId}")
    public ResponseEntity<Map<String, Object>> getWalletDetails(@PathVariable Long walletId) {
        Map<String, Object> res = new HashMap<>();
        try {
            Long userId = getCurrentUserId();
            Wallet wallet = walletService.getWalletDetails(userId, walletId);

            res.put("wallet", wallet);
            return ResponseEntity.ok(res);

        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.status(404).body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi máy chủ nội bộ: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    // ========================= SET DEFAULT WALLET =========================

    @PatchMapping("/{walletId}/set-default")
    @Transactional
    public ResponseEntity<Map<String, Object>> setDefaultWallet(@PathVariable Long walletId) {
        Map<String, Object> res = new HashMap<>();

        Long userId = getCurrentUserId();
        walletService.setDefaultWallet(userId, walletId);

        res.put("message", "Đặt ví mặc định thành công");
        return ResponseEntity.ok(res);
    }

    // ========================= SHARED WALLET ENDPOINTS =========================

    @PostMapping("/{walletId}/share")
    public ResponseEntity<Map<String, Object>> shareWallet(
            @PathVariable Long walletId,
            @Valid @RequestBody ShareWalletRequest request) {

        Map<String, Object> res = new HashMap<>();

        try {
            Long ownerId = getCurrentUserId();
            WalletMemberDTO member = walletService.shareWallet(walletId, ownerId, request.getEmail());

            res.put("message", "Chia sẻ ví thành công");
            res.put("member", member);
            return ResponseEntity.ok(res);

        } catch (RuntimeException ex) {
            res.put("error", ex.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    @GetMapping("/{walletId}/members")
    public ResponseEntity<Map<String, Object>> getWalletMembers(@PathVariable Long walletId) {
        Map<String, Object> res = new HashMap<>();

        try {
            Long userId = getCurrentUserId();
            List<WalletMemberDTO> members = walletService.getWalletMembers(walletId, userId);

            res.put("members", members);
            res.put("total", members.size());
            return ResponseEntity.ok(res);

        } catch (RuntimeException ex) {
            res.put("error", ex.getMessage());
            return ResponseEntity.status(403).body(res);
        }
    }

    @DeleteMapping("/{walletId}/members/{memberUserId}")
    public ResponseEntity<Map<String, Object>> removeMember(
            @PathVariable Long walletId, @PathVariable Long memberUserId) {

        Map<String, Object> res = new HashMap<>();

        try {
            Long ownerId = getCurrentUserId();
            walletService.removeMember(walletId, ownerId, memberUserId);

            res.put("message", "Xóa thành viên thành công");
            return ResponseEntity.ok(res);

        } catch (RuntimeException ex) {
            res.put("error", ex.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    @PostMapping("/{walletId}/leave")
    public ResponseEntity<Map<String, Object>> leaveWallet(@PathVariable Long walletId) {

        Map<String, Object> res = new HashMap<>();

        try {
            Long userId = getCurrentUserId();
            walletService.leaveWallet(walletId, userId);

            res.put("message", "Bạn đã rời khỏi ví thành công");
            return ResponseEntity.ok(res);

        } catch (RuntimeException ex) {
            res.put("error", ex.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }


    // ========================== ACCESS CHECK ==========================

    @GetMapping("/{walletId}/access")
    public ResponseEntity<Map<String, Object>> checkAccess(@PathVariable Long walletId) {
        Map<String, Object> res = new HashMap<>();

        Long userId = getCurrentUserId();

        boolean hasAccess = walletService.hasAccess(walletId, userId);
        boolean isOwner = walletService.isOwner(walletId, userId);

        res.put("hasAccess", hasAccess);
        res.put("isOwner", isOwner);
        res.put("role", isOwner ? "OWNER" : (hasAccess ? "MEMBER" : "NONE"));

        return ResponseEntity.ok(res);
    }

    // ========================== MERGE WALLET ==========================

    @GetMapping("/{sourceWalletId}/merge-candidates")
    public ResponseEntity<Map<String, Object>> getMergeCandidates(@PathVariable Long sourceWalletId) {
        Map<String, Object> res = new HashMap<>();

        try {
            Long userId = getCurrentUserId();
            List<MergeCandidateDTO> candidates = walletService.getMergeCandidates(userId, sourceWalletId);

            res.put("candidateWallets", candidates.stream().filter(MergeCandidateDTO::isCanMerge).toList());
            res.put("ineligibleWallets", candidates.stream().filter(c -> !c.isCanMerge()).toList());
            res.put("total", candidates.size());

            return ResponseEntity.ok(res);

        } catch (RuntimeException ex) {
            res.put("error", ex.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    @GetMapping("/{targetWalletId}/merge-preview")
    public ResponseEntity<Map<String, Object>> previewMerge(
            @PathVariable Long targetWalletId,
            @RequestParam Long sourceWalletId,
            @RequestParam String targetCurrency) {

        Map<String, Object> res = new HashMap<>();

        try {
            Long userId = getCurrentUserId();
            MergeWalletPreviewResponse preview = walletService.previewMerge(
                    userId, sourceWalletId, targetWalletId, targetCurrency
            );

            res.put("preview", preview);
            return ResponseEntity.ok(res);

        } catch (RuntimeException ex) {
            res.put("error", ex.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    @PostMapping("/{targetWalletId}/merge")
    @Transactional
    public ResponseEntity<Map<String, Object>> mergeWallets(
            @PathVariable Long targetWalletId,
            @Valid @RequestBody MergeWalletRequest request) {

        Map<String, Object> res = new HashMap<>();

        try {
            Long userId = getCurrentUserId();

            MergeWalletResponse result = walletService.mergeWallets(
                    userId,
                    request.getSourceWalletId(),
                    targetWalletId,
                    request.getTargetCurrency()
            );

            res.put("success", true);
            res.put("message", result.getMessage());
            res.put("result", result);

            return ResponseEntity.ok(res);

        } catch (RuntimeException ex) {
            res.put("success", false);
            res.put("error", ex.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    // ========================== UPDATE WALLET ==========================

    @PutMapping("/{walletId}")
    public ResponseEntity<Map<String, Object>> updateWallet(
            @PathVariable Long walletId,
            @Valid @RequestBody UpdateWalletRequest request) {

        Map<String, Object> res = new HashMap<>();

        try {
            Long userId = getCurrentUserId();

            // HEAD version: parameter order = (userId, walletId, request)
            Wallet updatedWallet = walletService.updateWallet(walletId, userId, request);

            res.put("message", "Cập nhật ví thành công");
            res.put("wallet", updatedWallet);

            return ResponseEntity.ok(res);

        } catch (RuntimeException ex) {
            res.put("error", ex.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    // ========================== DELETE WALLET ==========================

    @DeleteMapping("/{walletId}")
    public ResponseEntity<Map<String, Object>> deleteWallet(@PathVariable Long walletId) {

        Map<String, Object> res = new HashMap<>();

        try {
            Long userId = getCurrentUserId();

            DeleteWalletResponse result = walletService.deleteWallet(userId, walletId);

            res.put("message", "Xóa ví thành công");
            res.put("deletedWallet", result);

            return ResponseEntity.ok(res);

        } catch (RuntimeException ex) {
            res.put("error", ex.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    // ========================== TRANSFER MONEY ==========================

    @GetMapping("/{walletId}/transfer-targets")
    public ResponseEntity<Map<String, Object>> getTransferTargets(@PathVariable Long walletId) {
        Map<String, Object> res = new HashMap<>();

        try {
            Long userId = getCurrentUserId();
            Wallet sourceWallet = walletService.getWalletDetails(userId, walletId);

            List<SharedWalletDTO> allWallets = walletService.getAllAccessibleWallets(userId);

            List<SharedWalletDTO> targets = allWallets.stream()
                    .filter(w -> !w.getWalletId().equals(walletId))
                    .filter(w -> w.getCurrencyCode().equals(sourceWallet.getCurrencyCode()))
                    .toList();

            res.put("sourceWallet", Map.of(
                    "walletId", sourceWallet.getWalletId(),
                    "walletName", sourceWallet.getWalletName(),
                    "currencyCode", sourceWallet.getCurrencyCode(),
                    "balance", sourceWallet.getBalance()
            ));

            res.put("targetWallets", targets);
            res.put("total", targets.size());

            return ResponseEntity.ok(res);

        } catch (RuntimeException ex) {
            res.put("error", ex.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    @PostMapping("/transfer")
    public ResponseEntity<Map<String, Object>> transferMoney(
            @Valid @RequestBody TransferMoneyRequest request) {

        Map<String, Object> res = new HashMap<>();

        try {
            Long userId = getCurrentUserId();
            TransferMoneyResponse response = walletService.transferMoney(userId, request);

            res.put("message", "Chuyển tiền thành công");
            res.put("transfer", response);

            return ResponseEntity.ok(res);

        } catch (RuntimeException ex) {
            res.put("error", ex.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }
    // * Cập nhật thông tin ví (chỉ owner)
   //  * PUT /wallets/{walletId}/update
    // */
    @PutMapping("/{walletId}/update")
    public ResponseEntity<Map<String, Object>> updateWallet(
            @PathVariable Long walletId,
            @Valid @RequestBody UpdateWalletRequest request) {
        Map<String, Object> res = new HashMap<>();
        try {
            Long userId = getCurrentUserId();

            Wallet updatedWallet = walletService.updateWallet(walletId, userId, request);

            res.put("message", "Cập nhật ví thành công");
            res.put("wallet", updatedWallet);
            return ResponseEntity.ok(res);

        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi máy chủ nội bộ: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }
}
