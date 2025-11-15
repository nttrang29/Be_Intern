package com.example.financeapp.service;

import com.example.financeapp.dto.*;
import com.example.financeapp.entity.Wallet;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Map;

public interface WalletService {

    Wallet createWallet(Long userId, CreateWalletRequest request);
    Wallet updateWallet(Long walletId, Long userId, Map<String, Object> updates);
    Wallet updateWallet(Long walletId, Long userId, UpdateWalletRequest request);

    Wallet updateWallet(Long walletId, Long userId, Map<String, Object> updates);

    // ❗ Giữ lại method đúng signature
    Wallet updateWallet(Long userId, Long walletId, UpdateWalletRequest request);

    List<Wallet> getWalletsByUserId(Long userId);

    Wallet getWalletDetails(Long userId, Long walletId);

    // ============ SHARED WALLET METHODS ============
    @Transactional
    void setDefaultWallet(Long userId, Long walletId);

    List<SharedWalletDTO> getAllAccessibleWallets(Long userId);

    WalletMemberDTO shareWallet(Long walletId, Long ownerId, String memberEmail);

    List<WalletMemberDTO> getWalletMembers(Long walletId, Long requesterId);

    void removeMember(Long walletId, Long ownerId, Long memberUserId);

    void leaveWallet(Long walletId, Long userId);

    boolean hasAccess(Long walletId, Long userId);

    boolean isOwner(Long walletId, Long userId);

    // ============ MERGE WALLET METHODS ============

    List<MergeCandidateDTO> getMergeCandidates(Long userId, Long sourceWalletId);

    MergeWalletPreviewResponse previewMerge(
            Long userId,
            Long sourceWalletId,
            Long targetWalletId,
            String targetCurrency
    );

    @Transactional
    MergeWalletResponse mergeWallets(
            Long userId,
            Long sourceWalletId,
            Long targetWalletId,
            String targetCurrency
    );

    // ============ WALLET MANAGEMENT METHODS ============

    @Transactional
    DeleteWalletResponse deleteWallet(Long userId, Long walletId);

    // ============ MONEY TRANSFER METHODS ============

    @Transactional
    TransferMoneyResponse transferMoney(
            Long userId,
            TransferMoneyRequest request
    );
}
