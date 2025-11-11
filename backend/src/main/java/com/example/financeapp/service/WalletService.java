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
}