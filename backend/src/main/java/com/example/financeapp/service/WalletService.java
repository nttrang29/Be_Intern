package com.example.financeapp.service;

import com.example.financeapp.dto.CreateWalletRequest;
import com.example.financeapp.dto.SharedWalletDTO;
import com.example.financeapp.dto.WalletMemberDTO;
import com.example.financeapp.entity.Wallet;
import jakarta.transaction.Transactional;

import java.util.List;

public interface WalletService {

    Wallet createWallet(Long userId, CreateWalletRequest request);

    List<Wallet> getWalletsByUserId(Long userId);

    Wallet getWalletDetails(Long userId, Long walletId);

    // ============ SHARED WALLET METHODS ============
    @Transactional
    void setDefaultWallet(Long userId, Long walletId);
    /**
     * Lấy tất cả wallets mà user có quyền truy cập (bao gồm owned và shared)
     */
    List<SharedWalletDTO> getAllAccessibleWallets(Long userId);

    /**
     * Chia sẻ wallet với user khác qua email
     */
    WalletMemberDTO shareWallet(Long walletId, Long ownerId, String memberEmail);

    /**
     * Lấy danh sách members của một wallet
     */
    List<WalletMemberDTO> getWalletMembers(Long walletId, Long requesterId);

    /**
     * Xóa member khỏi wallet (chỉ owner)
     */
    void removeMember(Long walletId, Long ownerId, Long memberUserId);

    /**
     * Rời khỏi wallet (member tự rời)
     */
    void leaveWallet(Long walletId, Long userId);

    /**
     * Kiểm tra user có quyền truy cập wallet không
     */
    boolean hasAccess(Long walletId, Long userId);

    /**
     * Kiểm tra user có phải owner của wallet không
     */
    boolean isOwner(Long walletId, Long userId);

    // ============ MERGE WALLET METHODS ============

    /**
     * Lấy danh sách ví có thể gộp (cùng currency, không shared)
     */
    List<com.example.financeapp.dto.MergeCandidateDTO> getMergeCandidates(Long userId, Long sourceWalletId);

    /**
     * Preview kết quả merge trước khi thực hiện
     * Hỗ trợ xem trước với currency conversion
     */
    com.example.financeapp.dto.MergeWalletPreviewResponse previewMerge(
            Long userId,
            Long sourceWalletId,
            Long targetWalletId,
            String targetCurrency
    );

    /**
     * Thực hiện gộp ví với hỗ trợ currency conversion
     */
    @Transactional
    com.example.financeapp.dto.MergeWalletResponse mergeWallets(
            Long userId,
            Long sourceWalletId,
            Long targetWalletId,
            String targetCurrency
    );

    // ============ WALLET MANAGEMENT METHODS ============

    /**
     * Cập nhật thông tin ví (chỉ walletName và description)
     * Chỉ OWNER mới có quyền chỉnh sửa
     */
    Wallet updateWallet(Long userId, Long walletId, com.example.financeapp.dto.UpdateWalletRequest request);

    /**
     * Xóa ví và tất cả dữ liệu liên quan
     * Chỉ OWNER mới có quyền xóa
     * Tự động xử lý ví mặc định nếu cần
     */
    @Transactional
    com.example.financeapp.dto.DeleteWalletResponse deleteWallet(Long userId, Long walletId);

    // ============ MONEY TRANSFER METHODS ============

    /**
     * Chuyển tiền giữa các ví
     * Tạo 2 transactions: EXPENSE từ ví nguồn và INCOME vào ví đích
     * User phải có quyền truy cập cả 2 ví
     */
    @Transactional
    com.example.financeapp.dto.TransferMoneyResponse transferMoney(
            Long userId, 
            com.example.financeapp.dto.TransferMoneyRequest request
    );
}