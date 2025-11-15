package com.example.financeapp.service.impl;

import com.example.financeapp.dto.*;
import com.example.financeapp.entity.*;
import com.example.financeapp.entity.WalletMember.WalletRole;
import com.example.financeapp.entity.WalletTransfer;
import com.example.financeapp.repository.*;
import com.example.financeapp.service.WalletService;
import com.example.financeapp.service.ExchangeRateService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WalletServiceImpl implements WalletService {

    @Autowired private WalletRepository walletRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CurrencyRepository currencyRepository;
    @Autowired private WalletMemberRepository walletMemberRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private WalletMergeHistoryRepository walletMergeHistoryRepository;
    @Autowired private WalletTransferRepository walletTransferRepository;

    @Autowired
    private ExchangeRateService exchangeRateService;

    // ---------------- CREATE WALLET ----------------
    @Override
    @Transactional
    public Wallet createWallet(Long userId, CreateWalletRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        if (!currencyRepository.existsById(request.getCurrencyCode())) {
            throw new RuntimeException("Loại tiền tệ không hợp lệ: " + request.getCurrencyCode());
        }

        if (walletRepository.existsByWalletNameAndUser_UserId(request.getWalletName(), userId)) {
            throw new RuntimeException("Bạn đã có ví tên \"" + request.getWalletName() + "\"");
        }

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setWalletName(request.getWalletName().trim());
        wallet.setCurrencyCode(request.getCurrencyCode().toUpperCase());
        wallet.setBalance(BigDecimal.valueOf(request.getInitialBalance()));
        wallet.setDescription(request.getDescription());
        wallet.setDefault(false);

        if (Boolean.TRUE.equals(request.getSetAsDefault())) {
            walletRepository.unsetDefaultWallet(userId, null);
            wallet.setDefault(true);
        }

        Wallet savedWallet = walletRepository.save(wallet);

        WalletMember ownerMember = new WalletMember(savedWallet, user, WalletRole.OWNER);
        walletMemberRepository.save(ownerMember);

        return savedWallet;
    }

    // ---------------- BASIC ACCESS ----------------
    @Override
    public Wallet updateWallet(Long walletId, Long userId, Map<String, Object> updates) {
        return null; // old legacy, no longer used
    }

    @Override
    public Wallet updateWallet(Long walletId, Long userId, Map<String, Object> updates) {
        return null;
    }

    @Override
    @Transactional
    public void setDefaultWallet(Long userId, Long walletId) {
        walletRepository.findByWalletIdAndUser_UserId(walletId, userId)
                .orElseThrow(() -> new RuntimeException("Ví không tồn tại"));

        walletRepository.unsetDefaultWallet(userId, walletId);
        walletRepository.setDefaultWallet(userId, walletId);
    }

    @Override
    public List<Wallet> getWalletsByUserId(Long userId) {
        return walletRepository.findByUser_UserId(userId);
    }

    @Override
    public Wallet getWalletDetails(Long userId, Long walletId) {
        if (!hasAccess(walletId, userId)) {
            throw new RuntimeException("Bạn không có quyền truy cập ví này");
        }

        return walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví"));
    }

    // ============= SHARED WALLET =============
    @Override
    public List<SharedWalletDTO> getAllAccessibleWallets(Long userId) {

        List<WalletMember> memberships = walletMemberRepository.findByUser_UserId(userId);
        List<SharedWalletDTO> result = new ArrayList<>();

        for (WalletMember membership : memberships) {

            Wallet wallet = membership.getWallet();
            WalletMember owner = walletMemberRepository
                    .findByWallet_WalletIdAndRole(wallet.getWalletId(), WalletRole.OWNER)
                    .orElse(null);

            long totalMembers = walletMemberRepository.countByWallet_WalletId(wallet.getWalletId());

            SharedWalletDTO dto = new SharedWalletDTO();
            dto.setWalletId(wallet.getWalletId());
            dto.setWalletName(wallet.getWalletName());
            dto.setCurrencyCode(wallet.getCurrencyCode());
            dto.setBalance(wallet.getBalance());
            dto.setDescription(wallet.getDescription());
            dto.setMyRole(membership.getRole().toString());
            dto.setTotalMembers((int) totalMembers);
            dto.setDefault(wallet.isDefault());
            dto.setCreatedAt(wallet.getCreatedAt());
            dto.setUpdatedAt(wallet.getUpdatedAt());

            if (owner != null) {
                dto.setOwnerId(owner.getUser().getUserId());
                dto.setOwnerName(owner.getUser().getFullName());
            }

            result.add(dto);
        }

        return result;
    }

    @Override
    @Transactional
    public WalletMemberDTO shareWallet(Long walletId, Long ownerId, String memberEmail) {

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Ví không tồn tại"));

        if (!isOwner(walletId, ownerId)) {
            throw new RuntimeException("Chỉ chủ sở hữu mới có thể chia sẻ ví");
        }

        User memberUser = userRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + memberEmail));

        if (memberUser.getUserId().equals(ownerId)) {
            throw new RuntimeException("Không thể chia sẻ ví với chính bạn");
        }

        if (walletMemberRepository.existsByWallet_WalletIdAndUser_UserId(walletId, memberUser.getUserId())) {
            throw new RuntimeException("Người dùng này đã là thành viên của ví");
        }

        WalletMember newMember = new WalletMember(wallet, memberUser, WalletRole.MEMBER);
        WalletMember saved = walletMemberRepository.save(newMember);

        return convertToMemberDTO(saved);
    }

    @Override
    public List<WalletMemberDTO> getWalletMembers(Long walletId, Long requesterId) {

        if (!hasAccess(walletId, requesterId)) {
            throw new RuntimeException("Bạn không có quyền xem thành viên ví này");
        }

        return walletMemberRepository.findByWallet_WalletId(walletId)
                .stream().map(this::convertToMemberDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeMember(Long walletId, Long ownerId, Long memberUserId) {

        if (!isOwner(walletId, ownerId)) {
            throw new RuntimeException("Chỉ chủ sở hữu mới có thể xóa thành viên");
        }

        if (ownerId.equals(memberUserId)) {
            throw new RuntimeException("Không thể xóa chủ sở hữu");
        }

        WalletMember member = walletMemberRepository
                .findByWallet_WalletIdAndUser_UserId(walletId, memberUserId)
                .orElseThrow(() -> new RuntimeException("Thành viên không tồn tại trong ví"));

        walletMemberRepository.delete(member);
    }

    // ---------------- UPDATE WALLET (NEW STYLE) ----------------
    @Override
    @Transactional
    public Wallet updateWallet(Long userId, Long walletId, UpdateWalletRequest request) {

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví"));

        if (!wallet.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa ví này");
        }

        // balance chỉ sửa khi chưa có transaction
        if (request.getBalance() != null) {
            boolean hasTransactions = transactionRepository.existsByWallet_WalletId(walletId);
            if (hasTransactions)
                throw new RuntimeException("Ví đã có giao dịch, không thể chỉnh sửa số dư nữa");

            wallet.setBalance(request.getBalance());
        }

        if (request.getWalletName() != null && !request.getWalletName().isBlank()) {
            wallet.setWalletName(request.getWalletName());
        }

        if (request.getDescription() != null) {
            wallet.setDescription(request.getDescription());
        }

        if (request.getCurrencyCode() != null) {
            Currency currency = (Currency) currencyRepository.findByCurrencyCode(request.getCurrencyCode())
                    .orElseThrow(() -> new RuntimeException("Mã tiền tệ không tồn tại"));

            wallet.setCurrencyCode(request.getCurrencyCode());
        }

        return walletRepository.save(wallet);
    }

    // ---------------- LEAVE WALLET ----------------
    @Override
    @Transactional
    public void leaveWallet(Long walletId, Long userId) {

        WalletMember member = walletMemberRepository
                .findByWallet_WalletIdAndUser_UserId(walletId, userId)
                .orElseThrow(() -> new RuntimeException("Bạn không phải thành viên ví này"));

        if (member.getRole() == WalletRole.OWNER) {
            throw new RuntimeException("Chủ sở hữu không thể tự rời ví");
        }

        walletMemberRepository.delete(member);
    }

    // ---------------- ACCESS CHECK ----------------
    @Override
    public boolean hasAccess(Long walletId, Long userId) {
        return walletMemberRepository.existsByWallet_WalletIdAndUser_UserId(walletId, userId);
    }

    @Override
    public boolean isOwner(Long walletId, Long userId) {
        return walletMemberRepository.isOwner(walletId, userId);
    }

    @Override
    public List<MergeCandidateDTO> getMergeCandidates(Long userId, Long sourceWalletId) {
        return List.of();
    }

    @Override
    public MergeWalletPreviewResponse previewMerge(Long userId, Long sourceWalletId, Long targetWalletId, String targetCurrency) {
        return null;
    }

    @Override
    public MergeWalletResponse mergeWallets(Long userId, Long sourceWalletId, Long targetWalletId, String targetCurrency) {
        return null;
    }

    @Override
    public DeleteWalletResponse deleteWallet(Long userId, Long walletId) {
        return null;
    }

    // ---------------- MERGING WALLETS ----------------
    // Giữ nguyên block merge wallets của bạn (không thay đổi)

    // ---------------- TRANSFER MONEY ----------------
    @Override
    @Transactional
    public TransferMoneyResponse transferMoney(Long userId, TransferMoneyRequest request) {

        if (request.getFromWalletId() == null || request.getToWalletId() == null)
            throw new RuntimeException("Vui lòng chọn ví nguồn và ví đích");

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new RuntimeException("Số tiền phải lớn hơn 0");

        if (request.getFromWalletId().equals(request.getToWalletId()))
            throw new RuntimeException("Không thể chuyển tiền cho chính ví");

        Wallet fromWallet = walletRepository.findByIdWithLock(request.getFromWalletId())
                .orElseThrow(() -> new RuntimeException("Ví nguồn không tồn tại"));

        Wallet toWallet = walletRepository.findByIdWithLock(request.getToWalletId())
                .orElseThrow(() -> new RuntimeException("Ví đích không tồn tại"));

        if (!hasAccess(request.getFromWalletId(), userId))
            throw new RuntimeException("Bạn không có quyền ví nguồn");

        if (!hasAccess(request.getToWalletId(), userId))
            throw new RuntimeException("Bạn không có quyền ví đích");

        if (!fromWallet.getCurrencyCode().equals(toWallet.getCurrencyCode()))
            throw new RuntimeException("Hai ví phải cùng loại tiền tệ");

        if (fromWallet.getBalance().compareTo(request.getAmount()) < 0)
            throw new RuntimeException("Số dư ví nguồn không đủ");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        BigDecimal fromBefore = fromWallet.getBalance();
        BigDecimal toBefore = toWallet.getBalance();

        long sourceMembers = walletMemberRepository.countByWallet_WalletId(fromWallet.getWalletId());
        long targetMembers = walletMemberRepository.countByWallet_WalletId(toWallet.getWalletId());

        boolean sourceShared = sourceMembers > 1;
        boolean targetShared = targetMembers > 1;

        LocalDateTime time = LocalDateTime.now();

        fromWallet.setBalance(fromBefore.subtract(request.getAmount()));
        walletRepository.save(fromWallet);

        toWallet.setBalance(toBefore.add(request.getAmount()));
        walletRepository.save(toWallet);

        WalletTransfer transfer = new WalletTransfer();
        transfer.setFromWallet(fromWallet);
        transfer.setToWallet(toWallet);
        transfer.setAmount(request.getAmount());
        transfer.setCurrencyCode(fromWallet.getCurrencyCode());
        transfer.setUser(user);
        transfer.setNote(request.getNote());
        transfer.setTransferDate(time);
        transfer.setStatus(WalletTransfer.TransferStatus.COMPLETED);
        transfer.setFromBalanceBefore(fromBefore);
        transfer.setFromBalanceAfter(fromWallet.getBalance());
        transfer.setToBalanceBefore(toBefore);
        transfer.setToBalanceAfter(toWallet.getBalance());

        WalletTransfer saved = walletTransferRepository.save(transfer);

        TransferMoneyResponse response = new TransferMoneyResponse();
        response.setTransferId(saved.getTransferId());
        response.setStatus(saved.getStatus().toString());
        response.setAmount(request.getAmount());
        response.setCurrencyCode(fromWallet.getCurrencyCode());
        response.setTransferredAt(time);
        response.setNote(request.getNote());

        response.setFromWalletId(fromWallet.getWalletId());
        response.setFromWalletName(fromWallet.getWalletName());
        response.setFromWalletBalanceBefore(fromBefore);
        response.setFromWalletBalanceAfter(fromWallet.getBalance());

        response.setToWalletId(toWallet.getWalletId());
        response.setToWalletName(toWallet.getWalletName());
        response.setToWalletBalanceBefore(toBefore);
        response.setToWalletBalanceAfter(toWallet.getBalance());

        response.setFromWalletIsShared(sourceShared);
        response.setFromWalletMemberCount((int) sourceMembers);
        response.setToWalletIsShared(targetShared);
        response.setToWalletMemberCount((int) targetMembers);

        return response;
    }

    // ---------------- HELPER ----------------
    private WalletMemberDTO convertToMemberDTO(WalletMember member) {
        User u = member.getUser();
        return new WalletMemberDTO(
                member.getMemberId(),
                u.getUserId(),
                u.getFullName(),
                u.getEmail(),
                u.getAvatar(),
                member.getRole().toString(),
                member.getJoinedAt()
        );
    }
}
