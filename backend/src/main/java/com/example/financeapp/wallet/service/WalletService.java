// tập tin: .../service/WalletService.java
package com.example.financeapp.wallet.service;

import com.example.financeapp.wallet.dto.request.CreateWalletRequest;
import com.example.financeapp.wallet.dto.request.TransferMoneyRequest;
import com.example.financeapp.wallet.dto.request.UpdateTransferRequest;
import com.example.financeapp.wallet.dto.request.UpdateWalletRequest;
import com.example.financeapp.wallet.dto.response.DeleteWalletResponse;
import com.example.financeapp.wallet.dto.response.MergeCandidateDTO;
import com.example.financeapp.wallet.dto.response.MergeWalletPreviewResponse;
import com.example.financeapp.wallet.dto.response.MergeWalletResponse;
import com.example.financeapp.wallet.dto.response.SharedWalletDTO;
import com.example.financeapp.wallet.dto.response.TransferMoneyResponse;
import com.example.financeapp.wallet.dto.response.WalletMemberDTO;
import com.example.financeapp.wallet.dto.response.WalletTransactionHistoryDTO;
import com.example.financeapp.wallet.dto.response.WalletTransferHistoryDTO;
import com.example.financeapp.wallet.entity.Wallet;
import com.example.financeapp.wallet.entity.WalletTransfer;
import jakarta.transaction.Transactional;
import java.util.List;

// Interface này là "hợp đồng", định nghĩa rõ ràng các chức năng
public interface WalletService {

    Wallet createWallet(Long userId, CreateWalletRequest request);

    // ❗ CHỈ MỘT PHƯƠƠNG THỨC UPDATE DUY NHẤT
    // Chữ ký (signature) đúng: (userId, walletId, request)
    Wallet updateWallet(Long userId, Long walletId, UpdateWalletRequest request);

    List<Wallet> getWalletsByUserId(Long userId);

    Wallet getWalletDetails(Long userId, Long walletId);

    @Transactional
    void setDefaultWallet(Long userId, Long walletId);

    List<SharedWalletDTO> getAllAccessibleWallets(Long userId);

    WalletMemberDTO shareWallet(Long walletId, Long ownerId, String memberEmail);

    List<WalletMemberDTO> getWalletMembers(Long walletId, Long requesterId);

    void removeMember(Long walletId, Long ownerId, Long memberUserId);

    void leaveWallet(Long walletId, Long userId);

    /**
     * Cập nhật role cho một thành viên trong wallet.
     * @param walletId id của wallet
     * @param operatorUserId id của user thực hiện thay đổi (từ token)
     * @param memberUserId id của thành viên bị thay đổi
     * @param role giá trị role mới (OWNER hoặc MEMBER hoặc VIEW)
     */
    void updateMemberRole(Long walletId, Long operatorUserId, Long memberUserId, String role);

    boolean hasAccess(Long walletId, Long userId);

    boolean isOwner(Long walletId, Long userId);

    // ============ CÁC PHƯƠNG THỨC KHÁC ============
    List<MergeCandidateDTO> getMergeCandidates(Long userId, Long sourceWalletId);
    MergeWalletPreviewResponse previewMerge(Long userId, Long sourceWalletId, Long targetWalletId, String targetCurrency);
    @Transactional
    MergeWalletResponse mergeWallets(Long userId, Long sourceWalletId, Long targetWalletId, String targetCurrency);
    @Transactional
    DeleteWalletResponse deleteWallet(Long userId, Long walletId);
    @Transactional
    TransferMoneyResponse transferMoney(Long userId, TransferMoneyRequest request);

    List<WalletTransfer> getAllTransfers(Long userId);

    WalletTransfer updateTransfer(Long userId, Long transferId, UpdateTransferRequest request);

    @Transactional
    void deleteTransfer(Long userId, Long transferId);

    List<WalletTransactionHistoryDTO> getWalletTransactions(Long userId, Long walletId);

    List<WalletTransferHistoryDTO> getWalletTransfers(Long userId, Long walletId);
}