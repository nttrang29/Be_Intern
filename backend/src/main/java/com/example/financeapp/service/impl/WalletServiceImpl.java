package com.example.financeapp.service.impl;

import com.example.financeapp.dto.*;
import com.example.financeapp.entity.*;
import com.example.financeapp.entity.WalletMember.WalletRole;
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
import java.util.stream.Collectors;

@Service
public class WalletServiceImpl implements WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private WalletMemberRepository walletMemberRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletMergeHistoryRepository walletMergeHistoryRepository;

    @Autowired
    private TransactionTypeRepository transactionTypeRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Override
    @Transactional
    public Wallet createWallet(Long userId, CreateWalletRequest request) {
        // 1. Kiểm tra user tồn tại
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // 2. Kiểm tra loại tiền có hợp lệ
        if (!currencyRepository.existsById(request.getCurrencyCode())) {
            throw new RuntimeException("Loại tiền tệ không hợp lệ: " + request.getCurrencyCode());
        }

        // 3. Kiểm tra tên ví trùng (trong phạm vi user)
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
            walletRepository.unsetDefaultWallet(userId, null); // bỏ mặc định tất cả
            wallet.setDefault(true);
        }

        Wallet savedWallet = walletRepository.save(wallet);

        // 5. Tạo WalletMember với role OWNER
        WalletMember ownerMember = new WalletMember(savedWallet, user, WalletRole.OWNER);
        walletMemberRepository.save(ownerMember);

        return savedWallet;
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
        // Kiểm tra user có quyền truy cập wallet không (owner hoặc member)
        if (!hasAccess(walletId, userId)) {
            throw new RuntimeException("Bạn không có quyền truy cập ví này");
        }

        return walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví"));
    }

    // ============ SHARED WALLET IMPLEMENTATION ============

    @Override
    public List<SharedWalletDTO> getAllAccessibleWallets(Long userId) {
        // Lấy tất cả wallet memberships của user
        List<WalletMember> memberships = walletMemberRepository.findByUser_UserId(userId);

        List<SharedWalletDTO> result = new ArrayList<>();

        for (WalletMember membership : memberships) {
            Wallet wallet = membership.getWallet();

            // Tìm owner của wallet
            WalletMember owner = walletMemberRepository
                    .findByWallet_WalletIdAndRole(wallet.getWalletId(), WalletRole.OWNER)
                    .orElse(null);

            // Đếm tổng số members
            long totalMembers = walletMemberRepository.countByWallet_WalletId(wallet.getWalletId());

            SharedWalletDTO dto = new SharedWalletDTO();
            dto.setWalletId(wallet.getWalletId());
            dto.setWalletName(wallet.getWalletName());
            dto.setCurrencyCode(wallet.getCurrencyCode());
            dto.setBalance(wallet.getBalance());
            dto.setDescription(wallet.getDescription());
            dto.setMyRole(membership.getRole().toString());
            dto.setTotalMembers((int) totalMembers);
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
        // 1. Kiểm tra wallet tồn tại
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Ví không tồn tại"));

        // 2. Kiểm tra người share có phải owner không
        if (!isOwner(walletId, ownerId)) {
            throw new RuntimeException("Chỉ chủ sở hữu mới có thể chia sẻ ví");
        }

        // 3. Tìm user được chia sẻ qua email
        User memberUser = userRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + memberEmail));

        // 4. Kiểm tra không thể share với chính mình
        if (memberUser.getUserId().equals(ownerId)) {
            throw new RuntimeException("Không thể chia sẻ ví với chính bạn");
        }

        // 5. Kiểm tra user đã là member chưa
        if (walletMemberRepository.existsByWallet_WalletIdAndUser_UserId(walletId, memberUser.getUserId())) {
            throw new RuntimeException("Người dùng này đã là thành viên của ví");
        }

        // 6. Tạo WalletMember mới với role MEMBER
        WalletMember newMember = new WalletMember(wallet, memberUser, WalletRole.MEMBER);
        WalletMember savedMember = walletMemberRepository.save(newMember);

        // 7. Tạo DTO để trả về
        return convertToMemberDTO(savedMember);
    }

    @Override
    public List<WalletMemberDTO> getWalletMembers(Long walletId, Long requesterId) {
        // Kiểm tra requester có quyền truy cập wallet không
        if (!hasAccess(walletId, requesterId)) {
            throw new RuntimeException("Bạn không có quyền xem thành viên của ví này");
        }

        // Lấy danh sách members
        List<WalletMember> members = walletMemberRepository.findByWallet_WalletId(walletId);

        return members.stream()
                .map(this::convertToMemberDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeMember(Long walletId, Long ownerId, Long memberUserId) {
        // 1. Kiểm tra người xóa có phải owner không
        if (!isOwner(walletId, ownerId)) {
            throw new RuntimeException("Chỉ chủ sở hữu mới có thể xóa thành viên");
        }

        // 2. Không thể xóa chính mình (owner)
        if (ownerId.equals(memberUserId)) {
            throw new RuntimeException("Không thể xóa chủ sở hữu khỏi ví");
        }

        // 3. Kiểm tra member có trong wallet không
        WalletMember member = walletMemberRepository
                .findByWallet_WalletIdAndUser_UserId(walletId, memberUserId)
                .orElseThrow(() -> new RuntimeException("Thành viên không tồn tại trong ví này"));

        // 4. Không thể xóa owner khác (nếu có nhiều owner trong tương lai)
        if (member.getRole() == WalletRole.OWNER) {
            throw new RuntimeException("Không thể xóa chủ sở hữu");
        }

        // 5. Xóa member
        walletMemberRepository.delete(member);
    }

    @Override
    @Transactional
    public void leaveWallet(Long walletId, Long userId) {
        // 1. Kiểm tra user có trong wallet không
        WalletMember member = walletMemberRepository
                .findByWallet_WalletIdAndUser_UserId(walletId, userId)
                .orElseThrow(() -> new RuntimeException("Bạn không phải thành viên của ví này"));

        // 2. Owner không thể rời khỏi ví của mình
        if (member.getRole() == WalletRole.OWNER) {
            throw new RuntimeException("Chủ sở hữu không thể rời khỏi ví. Vui lòng xóa ví hoặc chuyển quyền sở hữu.");
        }

        // 3. Xóa member
        walletMemberRepository.delete(member);
    }

    @Override
    public boolean hasAccess(Long walletId, Long userId) {
        return walletMemberRepository.existsByWallet_WalletIdAndUser_UserId(walletId, userId);
    }

    @Override
    public boolean isOwner(Long walletId, Long userId) {
        return walletMemberRepository.isOwner(walletId, userId);
    }

    // ============ MERGE WALLET IMPLEMENTATION ============

    @Override
    public List<MergeCandidateDTO> getMergeCandidates(Long userId, Long sourceWalletId) {
        // 1. Lấy thông tin ví nguồn
        Wallet sourceWallet = walletRepository.findById(sourceWalletId)
                .orElseThrow(() -> new RuntimeException("Ví nguồn không tồn tại"));

        // 2. Kiểm tra user có phải owner không
        if (!isOwner(sourceWalletId, userId)) {
            throw new RuntimeException("Bạn không phải chủ sở hữu ví này");
        }

        // 3. Lấy tất cả memberships của user
        List<WalletMember> memberships = walletMemberRepository.findByUser_UserIdAndRole(userId, WalletRole.OWNER);

        List<MergeCandidateDTO> candidates = new ArrayList<>();

        for (WalletMember membership : memberships) {
            Wallet wallet = membership.getWallet();

            // Bỏ qua chính ví nguồn
            if (wallet.getWalletId().equals(sourceWalletId)) {
                continue;
            }

            // Đếm số members
            long memberCount = walletMemberRepository.countByWallet_WalletId(wallet.getWalletId());

            // Đếm số transactions
            int txCount = (int) transactionRepository.countByWallet_WalletId(wallet.getWalletId());

            MergeCandidateDTO candidate = new MergeCandidateDTO(
                    wallet.getWalletId(),
                    wallet.getWalletName(),
                    wallet.getCurrencyCode(),
                    wallet.getBalance(),
                    txCount,
                    wallet.isDefault()
            );

            // Kiểm tra điều kiện merge
            if (memberCount > 1) {
                candidate.setCanMerge(false);
                candidate.setReason("Ví đã được chia sẻ với " + (memberCount - 1) + " người khác");
            }
            // Không còn block khác currency - sẽ hiển thị warning ở frontend

            candidates.add(candidate);
        }

        // Sort: ví có thể merge trước, theo balance giảm dần
        candidates.sort((a, b) -> {
            if (a.isCanMerge() != b.isCanMerge()) {
                return a.isCanMerge() ? -1 : 1;
            }
            return b.getBalance().compareTo(a.getBalance());
        });

        return candidates;
    }

    @Override
    public MergeWalletPreviewResponse previewMerge(Long userId, Long sourceWalletId, Long targetWalletId, String targetCurrency) {
        // 1. Validate cơ bản
        Wallet sourceWallet = walletRepository.findById(sourceWalletId)
                .orElseThrow(() -> new RuntimeException("Ví nguồn không tồn tại"));

        Wallet targetWallet = walletRepository.findById(targetWalletId)
                .orElseThrow(() -> new RuntimeException("Ví đích không tồn tại"));

        // 2. Kiểm tra ownership
        if (!isOwner(sourceWalletId, userId) || !isOwner(targetWalletId, userId)) {
            throw new RuntimeException("Bạn không phải chủ sở hữu của cả 2 ví");
        }

        // 3. Kiểm tra không gộp với chính nó
        if (sourceWalletId.equals(targetWalletId)) {
            throw new RuntimeException("Không thể gộp ví với chính nó");
        }

        // 4. Validate targetCurrency
        if (targetCurrency == null || targetCurrency.trim().isEmpty()) {
            throw new RuntimeException("Loại tiền đích không được để trống");
        }
        targetCurrency = targetCurrency.toUpperCase();

        // Kiểm tra targetCurrency phải là 1 trong 2 currency của 2 ví
        if (!targetCurrency.equals(sourceWallet.getCurrencyCode()) && 
            !targetCurrency.equals(targetWallet.getCurrencyCode())) {
            throw new RuntimeException(
                "Loại tiền đích phải là " + sourceWallet.getCurrencyCode() + 
                " hoặc " + targetWallet.getCurrencyCode()
            );
        }

        // 5. Kiểm tra không shared
        long sourceMemberCount = walletMemberRepository.countByWallet_WalletId(sourceWalletId);
        long targetMemberCount = walletMemberRepository.countByWallet_WalletId(targetWalletId);

        if (sourceMemberCount > 1) {
            throw new RuntimeException("Không thể gộp ví đã được chia sẻ. Vui lòng xóa tất cả members trước.");
        }

        if (targetMemberCount > 1) {
            throw new RuntimeException("Không thể gộp vào ví đã được chia sẻ. Vui lòng chọn ví đích là ví cá nhân.");
        }

        // 6. Lấy transaction counts
        int sourceTxCount = (int) transactionRepository.countByWallet_WalletId(sourceWalletId);
        int targetTxCount = (int) transactionRepository.countByWallet_WalletId(targetWalletId);

        // 7. Tính toán conversion (nếu cần)
        BigDecimal sourceBalanceInTargetCurrency;
        BigDecimal finalBalance;
        BigDecimal exchangeRate = null;
        boolean needsConversion = !sourceWallet.getCurrencyCode().equals(targetCurrency);

        if (needsConversion) {
            // Lấy tỷ giá
            exchangeRate = exchangeRateService.getExchangeRate(
                sourceWallet.getCurrencyCode(), 
                targetCurrency
            );
            
            // Chuyển đổi balance của ví nguồn
            sourceBalanceInTargetCurrency = exchangeRateService.convertAmount(
                sourceWallet.getBalance(),
                sourceWallet.getCurrencyCode(),
                targetCurrency
            );
        } else {
            sourceBalanceInTargetCurrency = sourceWallet.getBalance();
        }

        // Tính final balance (có thể cần convert targetWallet balance nếu targetCurrency khác)
        BigDecimal targetBalanceInTargetCurrency;
        if (!targetWallet.getCurrencyCode().equals(targetCurrency)) {
            targetBalanceInTargetCurrency = exchangeRateService.convertAmount(
                targetWallet.getBalance(),
                targetWallet.getCurrencyCode(),
                targetCurrency
            );
        } else {
            targetBalanceInTargetCurrency = targetWallet.getBalance();
        }

        finalBalance = targetBalanceInTargetCurrency.add(sourceBalanceInTargetCurrency);

        // 8. Tạo preview response
        MergeWalletPreviewResponse preview = new MergeWalletPreviewResponse();

        // Source info
        preview.setSourceWalletId(sourceWalletId);
        preview.setSourceWalletName(sourceWallet.getWalletName());
        preview.setSourceCurrency(sourceWallet.getCurrencyCode());
        preview.setSourceBalance(sourceWallet.getBalance());
        preview.setSourceTransactionCount(sourceTxCount);
        preview.setSourceIsDefault(sourceWallet.isDefault());

        // Target info
        preview.setTargetWalletId(targetWalletId);
        preview.setTargetWalletName(targetWallet.getWalletName());
        preview.setTargetCurrency(targetWallet.getCurrencyCode());
        preview.setTargetBalance(targetWallet.getBalance());
        preview.setTargetTransactionCount(targetTxCount);

        // Final result
        preview.setFinalWalletName(targetWallet.getWalletName());
        preview.setFinalCurrency(targetCurrency);
        preview.setFinalBalance(finalBalance);
        preview.setTotalTransactions(sourceTxCount + targetTxCount);
        preview.setWillTransferDefaultFlag(sourceWallet.isDefault());

        // Warnings
        preview.addWarning("Ví '" + sourceWallet.getWalletName() + "' sẽ bị xóa vĩnh viễn");
        
        if (needsConversion && sourceTxCount > 0) {
            preview.addWarning(
                sourceTxCount + " giao dịch từ Ví " + sourceWallet.getCurrencyCode() + 
                " sẽ được chuyển đổi sang " + targetCurrency + 
                " theo tỷ giá: 1 " + sourceWallet.getCurrencyCode() + " = " + exchangeRate + " " + targetCurrency
            );
            preview.addWarning("Bạn vẫn có thể xem số tiền gốc (" + sourceWallet.getCurrencyCode() + ") của mỗi giao dịch");
            preview.addWarning("Tỷ giá có thể thay đổi. Tỷ giá hiển thị chỉ mang tính tham khảo");
        } else if (sourceTxCount > 0) {
            preview.addWarning(sourceTxCount + " giao dịch sẽ được chuyển sang ví đích");
        }
        
        if (sourceWallet.isDefault()) {
            preview.addWarning("Cờ 'Ví mặc định' sẽ chuyển sang ví đích");
        }
        preview.addWarning("Hành động này KHÔNG THỂ hoàn tác");

        preview.setCanProceed(true);

        return preview;
    }

    @Override
    @Transactional
    public MergeWalletResponse mergeWallets(Long userId, Long sourceWalletId, Long targetWalletId, String targetCurrency) {
        long startTime = System.currentTimeMillis();
        LocalDateTime mergeTime = LocalDateTime.now();

        // ===== VALIDATION =====
        Wallet sourceWallet = walletRepository.findById(sourceWalletId)
                .orElseThrow(() -> new RuntimeException("Ví nguồn không tồn tại"));

        Wallet targetWallet = walletRepository.findById(targetWalletId)
                .orElseThrow(() -> new RuntimeException("Ví đích không tồn tại"));

        if (!isOwner(sourceWalletId, userId) || !isOwner(targetWalletId, userId)) {
            throw new RuntimeException("Bạn không phải chủ sở hữu của cả 2 ví");
        }

        if (sourceWalletId.equals(targetWalletId)) {
            throw new RuntimeException("Không thể gộp ví với chính nó");
        }

        // Validate targetCurrency
        if (targetCurrency == null || targetCurrency.trim().isEmpty()) {
            throw new RuntimeException("Loại tiền đích không được để trống");
        }
        targetCurrency = targetCurrency.toUpperCase();

        if (!targetCurrency.equals(sourceWallet.getCurrencyCode()) && 
            !targetCurrency.equals(targetWallet.getCurrencyCode())) {
            throw new RuntimeException(
                "Loại tiền đích phải là " + sourceWallet.getCurrencyCode() + 
                " hoặc " + targetWallet.getCurrencyCode()
            );
        }

        long sourceMemberCount = walletMemberRepository.countByWallet_WalletId(sourceWalletId);
        long targetMemberCount = walletMemberRepository.countByWallet_WalletId(targetWalletId);

        if (sourceMemberCount > 1 || targetMemberCount > 1) {
            throw new RuntimeException("Chỉ có thể gộp các ví cá nhân (không chia sẻ)");
        }

        // ===== GATHER DATA =====
        int sourceTxCount = (int) transactionRepository.countByWallet_WalletId(sourceWalletId);
        int targetTxCount = (int) transactionRepository.countByWallet_WalletId(targetWalletId);
        BigDecimal oldTargetBalance = targetWallet.getBalance();
        boolean wasSourceDefault = sourceWallet.isDefault();

        // ===== TÍNH TOÁN CONVERSION (nếu cần) =====
        boolean needsSourceConversion = !sourceWallet.getCurrencyCode().equals(targetCurrency);
        boolean needsTargetConversion = !targetWallet.getCurrencyCode().equals(targetCurrency);
        BigDecimal exchangeRateSourceToTarget = null;
        BigDecimal exchangeRateTargetToTarget = null;

        // Convert source balance sang target currency
        BigDecimal sourceBalanceInTargetCurrency;
        if (needsSourceConversion) {
            exchangeRateSourceToTarget = exchangeRateService.getExchangeRate(
                sourceWallet.getCurrencyCode(),
                targetCurrency
            );
            sourceBalanceInTargetCurrency = exchangeRateService.convertAmount(
                sourceWallet.getBalance(),
                sourceWallet.getCurrencyCode(),
                targetCurrency
            );
        } else {
            sourceBalanceInTargetCurrency = sourceWallet.getBalance();
        }

        // Convert target balance sang target currency (nếu cần)
        BigDecimal targetBalanceInTargetCurrency;
        if (needsTargetConversion) {
            exchangeRateTargetToTarget = exchangeRateService.getExchangeRate(
                targetWallet.getCurrencyCode(),
                targetCurrency
            );
            targetBalanceInTargetCurrency = exchangeRateService.convertAmount(
                targetWallet.getBalance(),
                targetWallet.getCurrencyCode(),
                targetCurrency
            );
        } else {
            targetBalanceInTargetCurrency = targetWallet.getBalance();
        }

        // ===== CALCULATE NEW BALANCE =====
        BigDecimal newBalance = targetBalanceInTargetCurrency.add(sourceBalanceInTargetCurrency);

        // ===== SAVE MERGE HISTORY =====
        WalletMergeHistory history = new WalletMergeHistory();
        history.setUserId(userId);
        history.setSourceWalletId(sourceWalletId);
        history.setSourceWalletName(sourceWallet.getWalletName());
        history.setSourceCurrency(sourceWallet.getCurrencyCode());
        history.setSourceBalance(sourceWallet.getBalance());
        history.setSourceTransactionCount(sourceTxCount);
        history.setTargetWalletId(targetWalletId);
        history.setTargetWalletName(targetWallet.getWalletName());
        history.setTargetCurrency(targetWallet.getCurrencyCode());
        history.setTargetBalanceBefore(oldTargetBalance);
        history.setTargetBalanceAfter(newBalance);  // ✅ SET TRƯỚC KHI SAVE
        history.setTargetTransactionCountBefore(targetTxCount);

        WalletMergeHistory savedHistory = walletMergeHistoryRepository.save(history);

        // ===== PERFORM MERGE =====

        // 1. Chuyển transactions và convert currency nếu cần
        List<Transaction> sourceTransactions = transactionRepository.findByWallet_WalletId(sourceWalletId);
        int movedTx = 0;

        for (Transaction tx : sourceTransactions) {
            // Chuyển wallet
            tx.setWallet(targetWallet);
            
            // Nếu cần convert currency
            if (needsSourceConversion) {
                // Lưu thông tin gốc
                tx.setOriginalAmount(tx.getAmount());
                tx.setOriginalCurrency(sourceWallet.getCurrencyCode());
                tx.setExchangeRate(exchangeRateSourceToTarget);
                tx.setMergeDate(mergeTime);
                
                // Convert amount
                BigDecimal convertedAmount = exchangeRateService.convertAmount(
                    tx.getAmount(),
                    sourceWallet.getCurrencyCode(),
                    targetCurrency
                );
                tx.setAmount(convertedAmount);
            }
            
            transactionRepository.save(tx);
            movedTx++;
        }

        // 2. Cập nhật currency và balance của target wallet
        targetWallet.setCurrencyCode(targetCurrency);
        targetWallet.setBalance(newBalance);

        // 3. Xử lý default flag
        if (wasSourceDefault) {
            walletRepository.unsetDefaultWallet(userId, null);
            targetWallet.setDefault(true);
        }

        // 4. Save target wallet
        walletRepository.save(targetWallet);

        // 5. Delete source wallet_members TRƯỚC (tránh FK constraint)
        List<WalletMember> sourceMembers = walletMemberRepository.findByWallet_WalletId(sourceWalletId);
        walletMemberRepository.deleteAll(sourceMembers);

        // 6. Delete source wallet
        walletRepository.delete(sourceWallet);

        // 7. Update merge history duration
        long duration = System.currentTimeMillis() - startTime;
        savedHistory.setMergeDurationMs(duration);
        walletMergeHistoryRepository.save(savedHistory);

        // ===== CREATE RESPONSE =====
        MergeWalletResponse response = new MergeWalletResponse();
        response.setSuccess(true);
        
        if (needsSourceConversion) {
            response.setMessage(
                "Gộp ví thành công. Đã chuyển đổi " + movedTx + " giao dịch từ " + 
                sourceWallet.getCurrencyCode() + " sang " + targetCurrency
            );
        } else {
            response.setMessage("Gộp ví thành công");
        }
        
        response.setTargetWalletId(targetWalletId);
        response.setTargetWalletName(targetWallet.getWalletName());
        response.setFinalBalance(newBalance);
        response.setFinalCurrency(targetCurrency);
        response.setMergedTransactions(movedTx);
        response.setSourceWalletName(sourceWallet.getWalletName());
        response.setWasDefaultTransferred(wasSourceDefault);
        response.setMergeHistoryId(savedHistory.getMergeId());
        response.setMergedAt(savedHistory.getMergedAt());

        return response;
    }

    // ============ WALLET MANAGEMENT IMPLEMENTATION ============

    @Override
    @Transactional
    public Wallet updateWallet(Long userId, Long walletId, UpdateWalletRequest request) {
        // 1. Kiểm tra wallet tồn tại
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Ví không tồn tại"));

        // 2. Kiểm tra user là OWNER
        if (!isOwner(walletId, userId)) {
            throw new RuntimeException("Chỉ chủ sở hữu mới có quyền chỉnh sửa ví");
        }

        // 3. Validate input
        if (request.getWalletName() == null || request.getWalletName().trim().isEmpty()) {
            throw new RuntimeException("Tên ví không được để trống");
        }

        if (request.getWalletName().length() > 100) {
            throw new RuntimeException("Tên ví không được vượt quá 100 ký tự");
        }

        // 4. Kiểm tra tên ví trùng với ví khác của user (ngoại trừ ví hiện tại)
        boolean nameExists = walletRepository.existsByWalletNameAndUser_UserId(
                request.getWalletName().trim(), 
                userId
        );
        
        if (nameExists) {
            // Kiểm tra xem có phải đang đổi thành tên cũ không
            Wallet existingWallet = walletRepository.findByWalletNameAndUser_UserId(
                    request.getWalletName().trim(), 
                    userId
            );
            
            if (existingWallet != null && !existingWallet.getWalletId().equals(walletId)) {
                throw new RuntimeException("Bạn đã có ví tên \"" + request.getWalletName().trim() + "\"");
            }
        }

        // 5. Xử lý cập nhật balance (nếu có)
        if (request.getBalance() != null) {
            // Đếm số transactions trong ví
            long transactionCount = transactionRepository.countByWallet_WalletId(walletId);
            
            if (transactionCount > 0) {
                // Ví đã có giao dịch → KHÔNG cho phép sửa balance
                throw new RuntimeException(
                    "Không thể chỉnh sửa số dư khi ví đã có giao dịch. " +
                    "Ví này có " + transactionCount + " giao dịch. " +
                    "Số dư chỉ có thể thay đổi thông qua giao dịch hoặc bạn có thể xóa ví."
                );
            }
            
            // Ví chưa có giao dịch → CHO PHÉP sửa balance
            if (request.getBalance().compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("Số dư không được âm");
            }
            
            wallet.setBalance(request.getBalance());
        }

        // 6. Cập nhật thông tin khác
        wallet.setWalletName(request.getWalletName().trim());
        
        // Description có thể null
        if (request.getDescription() != null) {
            wallet.setDescription(request.getDescription().trim());
        } else {
            wallet.setDescription(null);
        }

        // 7. Save và return
        return walletRepository.save(wallet);
    }

    @Override
    @Transactional
    public DeleteWalletResponse deleteWallet(Long userId, Long walletId) {
        // 1. Kiểm tra wallet tồn tại
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Ví không tồn tại"));

        // 2. Kiểm tra user là OWNER
        if (!isOwner(walletId, userId)) {
            throw new RuntimeException("Chỉ chủ sở hữu mới có quyền xóa ví");
        }

        // 3. Thu thập thông tin trước khi xóa
        String walletName = wallet.getWalletName();
        BigDecimal balance = wallet.getBalance();
        String currencyCode = wallet.getCurrencyCode();
        boolean wasDefault = wallet.isDefault();

        // Đếm số transactions
        int transactionCount = (int) transactionRepository.countByWallet_WalletId(walletId);

        // Đếm số members (bao gồm owner)
        int memberCount = (int) walletMemberRepository.countByWallet_WalletId(walletId);

        // 4. Xử lý ví mặc định: nếu xóa ví default và user còn ví khác → đặt ví khác làm default
        Long newDefaultWalletId = null;
        String newDefaultWalletName = null;

        if (wasDefault) {
            // Tìm ví khác của user (không phải ví đang xóa)
            List<Wallet> otherWallets = walletRepository.findByUser_UserId(userId).stream()
                    .filter(w -> !w.getWalletId().equals(walletId))
                    .collect(Collectors.toList());

            if (!otherWallets.isEmpty()) {
                // Đặt ví đầu tiên trong list làm default
                Wallet newDefaultWallet = otherWallets.get(0);
                newDefaultWallet.setDefault(true);
                walletRepository.save(newDefaultWallet);

                newDefaultWalletId = newDefaultWallet.getWalletId();
                newDefaultWalletName = newDefaultWallet.getWalletName();
            }
        }

        // 5. Xóa wallet_members trước (tránh FK constraint)
        List<WalletMember> members = walletMemberRepository.findByWallet_WalletId(walletId);
        walletMemberRepository.deleteAll(members);

        // 6. Xóa tất cả transactions (hoặc có thể để CASCADE xử lý)
        // Note: Nếu đã có ON DELETE CASCADE trong DB thì không cần dòng này
        // transactionRepository.deleteByWallet_WalletId(walletId);

        // 7. Xóa wallet
        walletRepository.delete(wallet);

        // 8. Tạo response
        DeleteWalletResponse response = new DeleteWalletResponse();
        response.setDeletedWalletId(walletId);
        response.setDeletedWalletName(walletName);
        response.setBalance(balance);
        response.setCurrencyCode(currencyCode);
        response.setTransactionsDeleted(transactionCount);
        response.setMembersRemoved(memberCount);
        response.setWasDefault(wasDefault);
        response.setNewDefaultWalletId(newDefaultWalletId);
        response.setNewDefaultWalletName(newDefaultWalletName);

        return response;
    }

    // ============ MONEY TRANSFER IMPLEMENTATION ============

    @Override
    @Transactional
    public TransferMoneyResponse transferMoney(Long userId, TransferMoneyRequest request) {
        // ===== VALIDATION =====
        
        // 1. Validate input
        if (request.getFromWalletId() == null || request.getToWalletId() == null) {
            throw new RuntimeException("Vui lòng chọn ví nguồn và ví đích");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Số tiền phải lớn hơn 0");
        }

        // 2. Kiểm tra không chuyển cho chính mình
        if (request.getFromWalletId().equals(request.getToWalletId())) {
            throw new RuntimeException("Không thể chuyển tiền cho chính ví này");
        }

        // 3. Lấy thông tin 2 ví
        Wallet fromWallet = walletRepository.findById(request.getFromWalletId())
                .orElseThrow(() -> new RuntimeException("Ví nguồn không tồn tại"));

        Wallet toWallet = walletRepository.findById(request.getToWalletId())
                .orElseThrow(() -> new RuntimeException("Ví đích không tồn tại"));

        // 4. Kiểm tra user có quyền truy cập cả 2 ví không
        if (!hasAccess(request.getFromWalletId(), userId)) {
            throw new RuntimeException("Bạn không có quyền truy cập ví nguồn");
        }

        if (!hasAccess(request.getToWalletId(), userId)) {
            throw new RuntimeException("Bạn không có quyền truy cập ví đích");
        }

        // 5. Kiểm tra cùng loại tiền tệ
        if (!fromWallet.getCurrencyCode().equals(toWallet.getCurrencyCode())) {
            throw new RuntimeException(
                "Chỉ có thể chuyển tiền giữa các ví cùng loại tiền tệ. " +
                "Ví nguồn: " + fromWallet.getCurrencyCode() + ", Ví đích: " + toWallet.getCurrencyCode()
            );
        }

        // 6. Kiểm tra số dư ví nguồn
        if (fromWallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException(
                "Số dư ví nguồn không đủ. Số dư hiện tại: " + 
                fromWallet.getBalance() + " " + fromWallet.getCurrencyCode() +
                ", Số tiền chuyển: " + request.getAmount() + " " + fromWallet.getCurrencyCode()
            );
        }

        // 7. Lấy user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // 8. Lấy transaction types
        TransactionType expenseType = transactionTypeRepository.findByTypeName("Chi tiêu")
                .orElseThrow(() -> new RuntimeException("Transaction type 'Chi tiêu' không tồn tại"));

        TransactionType incomeType = transactionTypeRepository.findByTypeName("Thu nhập")
                .orElseThrow(() -> new RuntimeException("Transaction type 'Thu nhập' không tồn tại"));

        // 9. Lấy category
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category không tồn tại"));

        // ===== SAVE BALANCES BEFORE =====
        BigDecimal fromBalanceBefore = fromWallet.getBalance();
        BigDecimal toBalanceBefore = toWallet.getBalance();

        // ===== PERFORM TRANSFER =====
        LocalDateTime transferTime = LocalDateTime.now();

        // 1. Tạo transaction CHI TIÊU từ ví nguồn
        Transaction expenseTransaction = new Transaction();
        expenseTransaction.setUser(user);
        expenseTransaction.setWallet(fromWallet);
        expenseTransaction.setTransactionType(expenseType);
        expenseTransaction.setCategory(category);
        expenseTransaction.setAmount(request.getAmount());
        expenseTransaction.setTransactionDate(transferTime);
        expenseTransaction.setNote(
            (request.getNote() != null ? request.getNote() + " - " : "") +
            "Chuyển đến: " + toWallet.getWalletName()
        );
        Transaction savedExpense = transactionRepository.save(expenseTransaction);

        // 2. Cập nhật balance ví nguồn
        fromWallet.setBalance(fromWallet.getBalance().subtract(request.getAmount()));
        walletRepository.save(fromWallet);

        // 3. Tạo transaction THU NHẬP vào ví đích
        Transaction incomeTransaction = new Transaction();
        incomeTransaction.setUser(user);
        incomeTransaction.setWallet(toWallet);
        incomeTransaction.setTransactionType(incomeType);
        incomeTransaction.setCategory(category);
        incomeTransaction.setAmount(request.getAmount());
        incomeTransaction.setTransactionDate(transferTime);
        incomeTransaction.setNote(
            (request.getNote() != null ? request.getNote() + " - " : "") +
            "Nhận từ: " + fromWallet.getWalletName()
        );
        Transaction savedIncome = transactionRepository.save(incomeTransaction);

        // 4. Cập nhật balance ví đích
        toWallet.setBalance(toWallet.getBalance().add(request.getAmount()));
        walletRepository.save(toWallet);

        // ===== CREATE RESPONSE =====
        TransferMoneyResponse response = new TransferMoneyResponse();
        
        // General info
        response.setAmount(request.getAmount());
        response.setCurrencyCode(fromWallet.getCurrencyCode());
        response.setTransferredAt(transferTime);
        response.setNote(request.getNote());

        // From wallet info
        response.setFromWalletId(fromWallet.getWalletId());
        response.setFromWalletName(fromWallet.getWalletName());
        response.setFromWalletBalanceBefore(fromBalanceBefore);
        response.setFromWalletBalanceAfter(fromWallet.getBalance());
        response.setExpenseTransactionId(savedExpense.getTransactionId());

        // To wallet info
        response.setToWalletId(toWallet.getWalletId());
        response.setToWalletName(toWallet.getWalletName());
        response.setToWalletBalanceBefore(toBalanceBefore);
        response.setToWalletBalanceAfter(toWallet.getBalance());
        response.setIncomeTransactionId(savedIncome.getTransactionId());

        return response;
    }

    // ============ HELPER METHODS ============

    private WalletMemberDTO convertToMemberDTO(WalletMember member) {
        User user = member.getUser();
        return new WalletMemberDTO(
                member.getMemberId(),
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getAvatar(),
                member.getRole().toString(),
                member.getJoinedAt()
        );
    }
}
