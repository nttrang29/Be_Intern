package com.example.financeapp.fund.controller;

import com.example.financeapp.fund.dto.CreateFundRequest;
import com.example.financeapp.fund.dto.FundResponse;
import com.example.financeapp.fund.dto.UpdateFundRequest;
import com.example.financeapp.fund.service.FundService;
import com.example.financeapp.security.CustomUserDetails;
import com.example.financeapp.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/funds")
@CrossOrigin(origins = "*")
public class FundController {

    @Autowired
    private FundService fundService;

    /**
     * Tạo quỹ mới
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createFund(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateFundRequest request
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            FundResponse fund = fundService.createFund(user.getUserId(), request);

            res.put("message", "Tạo quỹ thành công");
            res.put("fund", fund);
            return ResponseEntity.ok(res);

        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Lấy tất cả quỹ của user
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllFunds(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            List<FundResponse> funds = fundService.getAllFunds(user.getUserId());

            res.put("funds", funds);
            res.put("total", funds.size());
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Lấy quỹ cá nhân
     */
    @GetMapping("/personal")
    public ResponseEntity<Map<String, Object>> getPersonalFunds(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Boolean hasDeadline
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            List<FundResponse> funds = fundService.getPersonalFunds(user.getUserId(), hasDeadline);

            res.put("funds", funds);
            res.put("total", funds.size());
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Lấy quỹ nhóm
     */
    @GetMapping("/group")
    public ResponseEntity<Map<String, Object>> getGroupFunds(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Boolean hasDeadline
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            List<FundResponse> funds = fundService.getGroupFunds(user.getUserId(), hasDeadline);

            res.put("funds", funds);
            res.put("total", funds.size());
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Lấy quỹ tham gia (không phải chủ quỹ)
     */
    @GetMapping("/participated")
    public ResponseEntity<Map<String, Object>> getParticipatedFunds(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            List<FundResponse> funds = fundService.getParticipatedFunds(user.getUserId());

            res.put("funds", funds);
            res.put("total", funds.size());
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Lấy chi tiết một quỹ
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getFundById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long fundId
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            FundResponse fund = fundService.getFundById(user.getUserId(), fundId);

            res.put("fund", fund);
            return ResponseEntity.ok(res);

        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Cập nhật quỹ
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateFund(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long fundId,
            @Valid @RequestBody UpdateFundRequest request
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            FundResponse fund = fundService.updateFund(user.getUserId(), fundId, request);

            res.put("message", "Cập nhật quỹ thành công");
            res.put("fund", fund);
            return ResponseEntity.ok(res);

        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Đóng quỹ
     */
    @PutMapping("/{id}/close")
    public ResponseEntity<Map<String, Object>> closeFund(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long fundId
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            fundService.closeFund(user.getUserId(), fundId);

            res.put("message", "Đóng quỹ thành công");
            return ResponseEntity.ok(res);

        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Xóa quỹ
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteFund(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long fundId
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            fundService.deleteFund(user.getUserId(), fundId);

            res.put("message", "Xóa quỹ thành công");
            return ResponseEntity.ok(res);

        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Nạp tiền vào quỹ
     */
    @PostMapping("/{id}/deposit")
    public ResponseEntity<Map<String, Object>> depositToFund(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long fundId,
            @RequestBody Map<String, Object> request
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            String message = request.containsKey("message") ? String.valueOf(request.get("message")) : null;
            boolean recovery = request.containsKey("recovery") && Boolean.TRUE.equals(request.get("recovery"));
            FundResponse fund = fundService.depositToFund(
                    user.getUserId(),
                    fundId,
                    amount,
                    recovery ? com.example.financeapp.fund.entity.FundTransactionType.AUTO_DEPOSIT_RECOVERY : com.example.financeapp.fund.entity.FundTransactionType.DEPOSIT,
                    message
            );

            res.put("message", "Nạp tiền vào quỹ thành công");
            res.put("fund", fund);
            return ResponseEntity.ok(res);

        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Rút tiền từ quỹ (chỉ cho quỹ không kỳ hạn)
     */
    @PostMapping("/{id}/withdraw")
    public ResponseEntity<Map<String, Object>> withdrawFromFund(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long fundId,
            @RequestBody Map<String, Object> request
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            FundResponse fund = fundService.withdrawFromFund(user.getUserId(), fundId, amount);

            res.put("message", "Rút tiền từ quỹ thành công");
            res.put("fund", fund);
            return ResponseEntity.ok(res);

        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Tất toán quỹ - rút toàn bộ số tiền còn lại về ví nguồn và đóng quỹ
     */
    @PostMapping("/{id}/settle")
    public ResponseEntity<Map<String, Object>> settleFund(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long fundId
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            FundResponse fund = fundService.settleFund(user.getUserId(), fundId);

            res.put("message", "Tất toán quỹ thành công");
            res.put("fund", fund);
            return ResponseEntity.ok(res);

        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Lịch sử giao dịch quỹ
     */
    @GetMapping("/{id}/transactions")
    public ResponseEntity<Map<String, Object>> getFundTransactions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long fundId,
            @RequestParam(name = "limit", required = false, defaultValue = "50") Integer limit
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userDetails.getUser();
            int safeLimit = (limit == null || limit <= 0) ? 50 : Math.min(limit, 200);
            var history = fundService.getFundTransactions(user.getUserId(), fundId, safeLimit);
            res.put("transactions", history);
            res.put("total", history.size());
            return ResponseEntity.ok(res);
        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Kiểm tra ví có đang được sử dụng không
     */
    @GetMapping("/check-wallet/{walletId}")
    public ResponseEntity<Map<String, Object>> checkWalletUsed(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("walletId") Long walletId
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            boolean isUsed = fundService.isWalletUsed(walletId);
            res.put("isUsed", isUsed);
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            res.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }
}

