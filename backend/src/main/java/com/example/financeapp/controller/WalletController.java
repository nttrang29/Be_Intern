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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy thông tin user từ token");
        }
        return userOpt.get().getUserId();
    }

    // ============ EXISTING ENDPOINTS ============

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

    @GetMapping
    public ResponseEntity<Map<String, Object>> getMyWallets() {
        Map<String, Object> res = new HashMap<>();
        try {
            Long userId = getCurrentUserId();

            // Lấy tất cả wallets có quyền truy cập (bao gồm owned và shared)
            List<SharedWalletDTO> wallets = walletService.getAllAccessibleWallets(userId);

            res.put("wallets", wallets);
            res.put("total", wallets.size());
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            res.put("error", e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

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

    @PatchMapping("/{walletId}/set-default")
    @Transactional
    public ResponseEntity<Map<String, Object>> setDefaultWallet(@PathVariable Long walletId) {
        Map<String, Object> res = new HashMap<>();
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Long userId = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User không tồn tại")).getUserId();

        walletService.setDefaultWallet(userId, walletId);

        res.put("message", "Đặt ví mặc định thành công");
        return ResponseEntity.ok(res);
    }
    // ============ SHARED WALLET ENDPOINTS ============

    /**
     * Chia sẻ ví với người dùng khác qua email
     * POST /wallets/{walletId}/share
     */
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

        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi máy chủ nội bộ: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Lấy danh sách thành viên của ví
     * GET /wallets/{walletId}/members
     */
    @GetMapping("/{walletId}/members")
    public ResponseEntity<Map<String, Object>> getWalletMembers(@PathVariable Long walletId) {
        Map<String, Object> res = new HashMap<>();
        try {
            Long userId = getCurrentUserId();

            List<WalletMemberDTO> members = walletService.getWalletMembers(walletId, userId);

            res.put("members", members);
            res.put("total", members.size());
            return ResponseEntity.ok(res);

        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.status(403).body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi máy chủ nội bộ: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Xóa thành viên khỏi ví (chỉ owner)
     * DELETE /wallets/{walletId}/members/{memberUserId}
     */
    @DeleteMapping("/{walletId}/members/{memberUserId}")
    public ResponseEntity<Map<String, Object>> removeMember(
            @PathVariable Long walletId,
            @PathVariable Long memberUserId) {
        Map<String, Object> res = new HashMap<>();
        try {
            Long ownerId = getCurrentUserId();

            walletService.removeMember(walletId, ownerId, memberUserId);

            res.put("message", "Xóa thành viên thành công");
            return ResponseEntity.ok(res);

        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi máy chủ nội bộ: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Rời khỏi ví (member tự rời)
     * POST /wallets/{walletId}/leave
     */
    @PostMapping("/{walletId}/leave")
    public ResponseEntity<Map<String, Object>> leaveWallet(@PathVariable Long walletId) {
        Map<String, Object> res = new HashMap<>();
        try {
            Long userId = getCurrentUserId();

            walletService.leaveWallet(walletId, userId);

            res.put("message", "Bạn đã rời khỏi ví thành công");
            return ResponseEntity.ok(res);

        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi máy chủ nội bộ: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Kiểm tra quyền truy cập của user đối với wallet
     * GET /wallets/{walletId}/access
     */
    @GetMapping("/{walletId}/access")
    public ResponseEntity<Map<String, Object>> checkAccess(@PathVariable Long walletId) {
        Map<String, Object> res = new HashMap<>();
        try {
            Long userId = getCurrentUserId();

            boolean hasAccess = walletService.hasAccess(walletId, userId);
            boolean isOwner = walletService.isOwner(walletId, userId);

            res.put("hasAccess", hasAccess);
            res.put("isOwner", isOwner);
            res.put("role", isOwner ? "OWNER" : (hasAccess ? "MEMBER" : "NONE"));

            return ResponseEntity.ok(res);

        } catch (Exception e) {
            res.put("error", "Lỗi máy chủ nội bộ: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    // ============ MERGE WALLET ENDPOINTS ============

    /**
     * Lấy danh sách ví có thể gộp
     * GET /wallets/{sourceWalletId}/merge-candidates
     */
    @GetMapping("/{sourceWalletId}/merge-candidates")
    public ResponseEntity<Map<String, Object>> getMergeCandidates(@PathVariable Long sourceWalletId) {
        Map<String, Object> res = new HashMap<>();
        try {
            Long userId = getCurrentUserId();

            List<MergeCandidateDTO> candidates = walletService.getMergeCandidates(userId, sourceWalletId);

            // Tách ra eligible và ineligible
            List<MergeCandidateDTO> eligible = candidates.stream()
                    .filter(MergeCandidateDTO::isCanMerge)
                    .collect(java.util.stream.Collectors.toList());

            List<MergeCandidateDTO> ineligible = candidates.stream()
                    .filter(c -> !c.isCanMerge())
                    .collect(java.util.stream.Collectors.toList());

            res.put("candidateWallets", eligible);
            res.put("ineligibleWallets", ineligible);
            res.put("total", eligible.size());
            return ResponseEntity.ok(res);

        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi máy chủ nội bộ: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Preview kết quả merge
     * GET /wallets/{targetWalletId}/merge-preview?sourceWalletId=X&targetCurrency=VND
     */
    @GetMapping("/{targetWalletId}/merge-preview")
    public ResponseEntity<Map<String, Object>> previewMerge(
            @PathVariable Long targetWalletId,
            @RequestParam Long sourceWalletId,
            @RequestParam String targetCurrency) {
        Map<String, Object> res = new HashMap<>();
        try {
            Long userId = getCurrentUserId();

            MergeWalletPreviewResponse preview = walletService.previewMerge(
                    userId,
                    sourceWalletId,
                    targetWalletId,
                    targetCurrency
            );

            res.put("preview", preview);
            return ResponseEntity.ok(res);

        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi máy chủ nội bộ: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Thực hiện gộp ví với hỗ trợ currency conversion
     * POST /wallets/{targetWalletId}/merge
     */
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

        } catch (RuntimeException e) {
            res.put("success", false);
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("error", "Lỗi máy chủ nội bộ: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Lấy lịch sử merge của user
     * GET /wallets/merge-history
     */
    @GetMapping("/merge-history")
    public ResponseEntity<Map<String, Object>> getMergeHistory() {
        Map<String, Object> res = new HashMap<>();
        try {
            Long userId = getCurrentUserId();

            // Note: Cần inject WalletMergeHistoryRepository vào controller
            // Hoặc tạo method trong WalletService
            // Tạm thời return empty list
            res.put("history", new java.util.ArrayList<>());
            res.put("total", 0);
            res.put("message", "Feature coming soon");
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            res.put("error", e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    // ============ WALLET MANAGEMENT ENDPOINTS ============

    /**
     * Cập nhật thông tin ví (chỉ tên và mô tả)
     * PUT /wallets/{walletId}
     */
    @PutMapping("/{walletId}")
    public ResponseEntity<Map<String, Object>> updateWallet(
            @PathVariable Long walletId,
            @Valid @RequestBody UpdateWalletRequest request) {
        Map<String, Object> res = new HashMap<>();
        try {
            Long userId = getCurrentUserId();

            Wallet updatedWallet = walletService.updateWallet(userId, walletId, request);

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

    /**
     * Xóa ví
     * DELETE /wallets/{walletId}
     */
    @DeleteMapping("/{walletId}")
    public ResponseEntity<Map<String, Object>> deleteWallet(@PathVariable Long walletId) {
        Map<String, Object> res = new HashMap<>();
        try {
            Long userId = getCurrentUserId();

            DeleteWalletResponse deleteResponse = walletService.deleteWallet(userId, walletId);

            res.put("message", "Xóa ví thành công");
            res.put("deletedWallet", deleteResponse);
            return ResponseEntity.ok(res);

        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi máy chủ nội bộ: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    // ============ MONEY TRANSFER ENDPOINTS ============

    /**
     * Lấy danh sách ví có thể chuyển tiền đến từ ví nguồn
     * GET /wallets/{walletId}/transfer-targets
     */
    @GetMapping("/{walletId}/transfer-targets")
    public ResponseEntity<Map<String, Object>> getTransferTargets(@PathVariable Long walletId) {
        Map<String, Object> res = new HashMap<>();
        try {
            Long userId = getCurrentUserId();

            // Lấy ví nguồn để biết currency
            Wallet sourceWallet = walletService.getWalletDetails(userId, walletId);
            
            // Lấy tất cả ví có quyền truy cập
            List<SharedWalletDTO> allWallets = walletService.getAllAccessibleWallets(userId);
            
            // Filter: Bỏ ví nguồn và chỉ lấy ví cùng currency
            List<SharedWalletDTO> targets = allWallets.stream()
                    .filter(w -> !w.getWalletId().equals(walletId)) // Không phải ví nguồn
                    .filter(w -> w.getCurrencyCode().equals(sourceWallet.getCurrencyCode())) // Cùng currency
                    .collect(java.util.stream.Collectors.toList());

            res.put("sourceWallet", Map.of(
                "walletId", sourceWallet.getWalletId(),
                "walletName", sourceWallet.getWalletName(),
                "currencyCode", sourceWallet.getCurrencyCode(),
                "balance", sourceWallet.getBalance()
            ));
            res.put("targetWallets", targets);
            res.put("total", targets.size());
            return ResponseEntity.ok(res);

        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi máy chủ nội bộ: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Chuyển tiền giữa các ví
     * POST /wallets/transfer
     */
    @PostMapping("/transfer")
    public ResponseEntity<Map<String, Object>> transferMoney(
            @Valid @RequestBody TransferMoneyRequest request) {
        Map<String, Object> res = new HashMap<>();
        try {
            Long userId = getCurrentUserId();

            TransferMoneyResponse transferResponse = walletService.transferMoney(userId, request);

            res.put("message", "Chuyển tiền thành công");
            res.put("transfer", transferResponse);
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
