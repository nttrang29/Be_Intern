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

        if ("GROUP".equalsIgnoreCase(request.getWalletType())) {
            wallet.setWalletType("GROUP");
        } else {
            wallet.setWalletType("PERSONAL"); // Mặc định là cá nhân
        }
        
        if (Boolean.TRUE.equals(request.getSetAsDefault())) {
            walletRepository.unsetDefaultWallet(userId, null);
            wallet.setDefault(true);
        }

        Wallet savedWallet = walletRepository.save(wallet);

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
            dto.setWalletType(wallet.getWalletType());
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

        // ❗ ĐÂY LÀ KHAI BÁO BIẾN "wallet" MÀ BẠN BỊ THIẾU
        // Khai báo biến 'wallet' ngay đầu scope của phương thức
        // Bằng cách lấy nó từ repository
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví"));

        // Bây giờ bạn có thể sử dụng biến "wallet"
        if (!wallet.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa ví này");
        }

        // Logic cũ của bạn (cập nhật balance)
        if (request.getBalance() != null) {
            boolean hasTransactions = transactionRepository.existsByWallet_WalletId(walletId);
            if (hasTransactions)
                throw new RuntimeException("Ví đã có giao dịch, không thể chỉnh sửa số dư nữa");
            wallet.setBalance(request.getBalance());
        }

        // Cập nhật tên
        if (request.getWalletName() != null && !request.getWalletName().isBlank()) {
            wallet.setWalletName(request.getWalletName());
        }

        // Cập nhật mô tả
        if (request.getDescription() != null) {
            wallet.setDescription(request.getDescription());
        }

        // Cập nhật tiền tệ (File gốc của bạn thiếu, tôi bổ sung)
        if (request.getCurrencyCode() != null) {
            if (!currencyRepository.existsById(request.getCurrencyCode())) { // Giả sử bạn có currencyRepository
                throw new RuntimeException("Mã tiền tệ không tồn tại");
            }
            wallet.setCurrencyCode(request.getCurrencyCode());
        }

        // Cập nhật ví mặc định (sử dụng DTO đã sửa)
        if (Boolean.TRUE.equals(request.getSetAsDefault())) {
            walletRepository.unsetDefaultWallet(userId, walletId);
            wallet.setDefault(true);
        } else if (Boolean.FALSE.equals(request.getSetAsDefault())) {
            // Nếu setAsDefault = false, bỏ ví mặc định
            wallet.setDefault(false);
        }

        // Cập nhật loại ví (PERSONAL <-> GROUP)
        if (request.getWalletType() != null && !request.getWalletType().isBlank()) {
            String newWalletType = request.getWalletType().toUpperCase();
            
            // Validate wallet type
            if (!"PERSONAL".equals(newWalletType) && !"GROUP".equals(newWalletType)) {
                throw new RuntimeException("Loại ví không hợp lệ. Chỉ chấp nhận PERSONAL hoặc GROUP");
            }
            
            String currentWalletType = wallet.getWalletType();
            
            // Cho phép chuyển PERSONAL -> GROUP
            if ("PERSONAL".equals(currentWalletType) && "GROUP".equals(newWalletType)) {
                wallet.setWalletType("GROUP");
                
                // Đảm bảo owner được thêm vào WalletMember nếu chưa có
                boolean ownerExists = walletMemberRepository.existsByWallet_WalletIdAndUser_UserId(
                    walletId, userId
                );
                
                if (!ownerExists) {
                    User owner = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User không tồn tại"));
                    WalletMember ownerMember = new WalletMember(wallet, owner, WalletRole.OWNER);
                    walletMemberRepository.save(ownerMember);
                }
            }
            // Không cho phép chuyển GROUP -> PERSONAL
            else if ("GROUP".equals(currentWalletType) && "PERSONAL".equals(newWalletType)) {
                throw new RuntimeException("Không thể chuyển ví nhóm về ví cá nhân. Vui lòng xóa các thành viên trước.");
            }
            // Nếu cùng loại thì không cần làm gì
            // (hoặc có thể cho phép giữ nguyên)
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

    // ---------------- MERGE WALLET ---------------- 
    @Override
    public List<MergeCandidateDTO> getMergeCandidates(Long userId, Long sourceWalletId) {
        // Kiểm tra quyền sở hữu source wallet
        if (!isOwner(sourceWalletId, userId)) {
            throw new RuntimeException("Bạn không có quyền gộp ví này");
        }

        // Kiểm tra ví nguồn tồn tại
        if (!walletRepository.existsById(sourceWalletId)) {
            throw new RuntimeException("Ví nguồn không tồn tại");
        }

        // Lấy tất cả ví của user (không bao gồm source wallet)
        List<SharedWalletDTO> allWallets = getAllAccessibleWallets(userId);
        
        List<MergeCandidateDTO> candidates = new ArrayList<>();
        
        for (SharedWalletDTO wallet : allWallets) {
            if (wallet.getWalletId().equals(sourceWalletId)) {
                continue; // Bỏ qua chính ví nguồn
            }

            // Chỉ owner mới có thể merge
            if (!isOwner(wallet.getWalletId(), userId)) {
                continue;
            }

            MergeCandidateDTO candidate = new MergeCandidateDTO();
            candidate.setWalletId(wallet.getWalletId());
            candidate.setWalletName(wallet.getWalletName());
            candidate.setCurrencyCode(wallet.getCurrencyCode());
            candidate.setBalance(wallet.getBalance());
            candidate.setDefault(wallet.isDefault());
            
            // Đếm số transactions
            long transactionCount = transactionRepository.countByWallet_WalletId(wallet.getWalletId());
            candidate.setTransactionCount((int) transactionCount);

            // Có thể merge nếu:
            // - Không phải cùng ví
            // - User là owner của cả 2 ví
            candidate.setCanMerge(true);
            candidate.setReason(null);
            
            candidates.add(candidate);
        }

        return candidates;
    }

    @Override
    public MergeWalletPreviewResponse previewMerge(Long userId, Long sourceWalletId, Long targetWalletId, String targetCurrency) {
        // Kiểm tra quyền sở hữu
        if (!isOwner(sourceWalletId, userId)) {
            throw new RuntimeException("Bạn không có quyền gộp ví nguồn này");
        }
        if (!isOwner(targetWalletId, userId)) {
            throw new RuntimeException("Bạn không có quyền gộp vào ví đích này");
        }

        if (sourceWalletId.equals(targetWalletId)) {
            throw new RuntimeException("Không thể gộp ví với chính nó");
        }

        Wallet sourceWallet = walletRepository.findById(sourceWalletId)
                .orElseThrow(() -> new RuntimeException("Ví nguồn không tồn tại"));
        
        Wallet targetWallet = walletRepository.findById(targetWalletId)
                .orElseThrow(() -> new RuntimeException("Ví đích không tồn tại"));

        // Validate currency
        if (!currencyRepository.existsById(targetCurrency)) {
            throw new RuntimeException("Loại tiền tệ không hợp lệ: " + targetCurrency);
        }

        // Đếm transactions
        long sourceTransactionCount = transactionRepository.countByWallet_WalletId(sourceWalletId);
        long targetTransactionCount = transactionRepository.countByWallet_WalletId(targetWalletId);

        // Chuyển đổi số dư nếu cần
        BigDecimal sourceBalanceConverted = sourceWallet.getBalance();
        if (!sourceWallet.getCurrencyCode().equals(targetCurrency)) {
            sourceBalanceConverted = exchangeRateService.convertAmount(
                sourceWallet.getBalance(),
                sourceWallet.getCurrencyCode(),
                targetCurrency
            );
        }

        BigDecimal targetBalanceConverted = targetWallet.getBalance();
        if (!targetWallet.getCurrencyCode().equals(targetCurrency)) {
            targetBalanceConverted = exchangeRateService.convertAmount(
                targetWallet.getBalance(),
                targetWallet.getCurrencyCode(),
                targetCurrency
            );
        }

        BigDecimal finalBalance = sourceBalanceConverted.add(targetBalanceConverted);

        // Tạo preview response
        MergeWalletPreviewResponse preview = new MergeWalletPreviewResponse();
        preview.setSourceWalletId(sourceWalletId);
        preview.setSourceWalletName(sourceWallet.getWalletName());
        preview.setSourceCurrency(sourceWallet.getCurrencyCode());
        preview.setSourceBalance(sourceWallet.getBalance());
        preview.setSourceTransactionCount((int) sourceTransactionCount);
        preview.setSourceIsDefault(sourceWallet.isDefault());

        preview.setTargetWalletId(targetWalletId);
        preview.setTargetWalletName(targetWallet.getWalletName());
        preview.setTargetCurrency(targetWallet.getCurrencyCode());
        preview.setTargetBalance(targetWallet.getBalance());
        preview.setTargetTransactionCount((int) targetTransactionCount);

        preview.setFinalWalletName(targetWallet.getWalletName());
        preview.setFinalCurrency(targetCurrency);
        preview.setFinalBalance(finalBalance);
        preview.setTotalTransactions((int) (sourceTransactionCount + targetTransactionCount));
        preview.setWillTransferDefaultFlag(sourceWallet.isDefault());

        // Validation
        preview.setCanProceed(true);
        List<String> warnings = new ArrayList<>();
        
        if (!sourceWallet.getCurrencyCode().equals(targetCurrency) || 
            !targetWallet.getCurrencyCode().equals(targetCurrency)) {
            warnings.add("Số dư sẽ được chuyển đổi sang " + targetCurrency);
        }
        
        if (sourceWallet.isDefault()) {
            warnings.add("Ví mặc định sẽ được chuyển sang ví đích");
        }

        preview.setWarnings(warnings);

        return preview;
    }

    @Override
    @Transactional
    public MergeWalletResponse mergeWallets(Long userId, Long sourceWalletId, Long targetWalletId, String targetCurrency) {
        long startTime = System.currentTimeMillis();

        // Kiểm tra quyền sở hữu
        if (!isOwner(sourceWalletId, userId)) {
            throw new RuntimeException("Bạn không có quyền gộp ví nguồn này");
        }
        if (!isOwner(targetWalletId, userId)) {
            throw new RuntimeException("Bạn không có quyền gộp vào ví đích này");
        }

        if (sourceWalletId.equals(targetWalletId)) {
            throw new RuntimeException("Không thể gộp ví với chính nó");
        }

        Wallet sourceWallet = walletRepository.findByIdWithLock(sourceWalletId)
                .orElseThrow(() -> new RuntimeException("Ví nguồn không tồn tại"));
        
        Wallet targetWallet = walletRepository.findByIdWithLock(targetWalletId)
                .orElseThrow(() -> new RuntimeException("Ví đích không tồn tại"));

        // Validate currency
        if (!currencyRepository.existsById(targetCurrency)) {
            throw new RuntimeException("Loại tiền tệ không hợp lệ: " + targetCurrency);
        }

        // Lưu thông tin trước khi merge
        String sourceWalletName = sourceWallet.getWalletName();
        String sourceCurrency = sourceWallet.getCurrencyCode();
        BigDecimal sourceBalance = sourceWallet.getBalance();
        int sourceTransactionCount = (int) transactionRepository.countByWallet_WalletId(sourceWalletId);
        
        BigDecimal targetBalanceBefore = targetWallet.getBalance();
        int targetTransactionCountBefore = (int) transactionRepository.countByWallet_WalletId(targetWalletId);
        boolean wasSourceDefault = sourceWallet.isDefault();

        // Chuyển đổi số dư source wallet sang target currency
        BigDecimal sourceBalanceConverted = sourceBalance;
        if (!sourceCurrency.equals(targetCurrency)) {
            sourceBalanceConverted = exchangeRateService.convertAmount(
                sourceBalance,
                sourceCurrency,
                targetCurrency
            );
        }

        // Chuyển đổi số dư target wallet sang target currency (nếu cần)
        BigDecimal targetBalanceConverted = targetBalanceBefore;
        if (!targetWallet.getCurrencyCode().equals(targetCurrency)) {
            targetBalanceConverted = exchangeRateService.convertAmount(
                targetBalanceBefore,
                targetWallet.getCurrencyCode(),
                targetCurrency
            );
        }

        // Cập nhật target wallet
        targetWallet.setCurrencyCode(targetCurrency);
        targetWallet.setBalance(sourceBalanceConverted.add(targetBalanceConverted));
        
        // Nếu source wallet là default, chuyển sang target wallet
        if (wasSourceDefault) {
            walletRepository.unsetDefaultWallet(userId, targetWalletId);
            targetWallet.setDefault(true);
        }

        walletRepository.save(targetWallet);

        // Chuyển tất cả transactions từ source sang target
        List<Transaction> sourceTransactions = transactionRepository.findByWallet_WalletId(sourceWalletId);
        LocalDateTime mergeDate = LocalDateTime.now();
        
        // Lưu currency gốc của source wallet trước khi thay đổi
        String originalSourceCurrency = sourceWallet.getCurrencyCode();
        
        for (Transaction tx : sourceTransactions) {
            // Nếu transaction có currency khác với target currency, lưu thông tin gốc
            if (!originalSourceCurrency.equals(targetCurrency)) {
                tx.setOriginalAmount(tx.getAmount());
                tx.setOriginalCurrency(originalSourceCurrency);
                
                // Chuyển đổi amount sang target currency
                BigDecimal convertedAmount = exchangeRateService.convertAmount(
                    tx.getAmount(),
                    originalSourceCurrency,
                    targetCurrency
                );
                tx.setAmount(convertedAmount);
                
                // Lưu exchange rate
                BigDecimal rate = exchangeRateService.getExchangeRate(
                    originalSourceCurrency,
                    targetCurrency
                );
                tx.setExchangeRate(rate);
            }
            
            tx.setWallet(targetWallet);
            tx.setMergeDate(mergeDate);
            transactionRepository.save(tx);
        }

        // Chuyển tất cả members từ source sang target (nếu chưa có)
        List<WalletMember> sourceMembers = walletMemberRepository.findByWallet_WalletId(sourceWalletId);
        for (WalletMember member : sourceMembers) {
            // Kiểm tra xem member đã có trong target wallet chưa
            boolean existsInTarget = walletMemberRepository.existsByWallet_WalletIdAndUser_UserId(
                targetWalletId,
                member.getUser().getUserId()
            );
            
            if (!existsInTarget) {
                WalletMember newMember = new WalletMember(
                    targetWallet,
                    member.getUser(),
                    WalletRole.MEMBER // Luôn là MEMBER vì target wallet đã có owner
                );
                walletMemberRepository.save(newMember);
            }
        }

        // Xóa source wallet members
        walletMemberRepository.deleteAll(sourceMembers);

        // Xóa source wallet transfers
        walletTransferRepository.deleteByFromWallet_WalletIdOrToWallet_WalletId(
            sourceWalletId,
            sourceWalletId
        );

        // Xóa source wallet
        walletRepository.delete(sourceWallet);

        // Lưu lịch sử merge
        WalletMergeHistory history = new WalletMergeHistory();
        history.setUserId(userId);
        history.setSourceWalletId(sourceWalletId);
        history.setSourceWalletName(sourceWalletName);
        history.setSourceCurrency(sourceCurrency);
        history.setSourceBalance(sourceBalance);
        history.setSourceTransactionCount(sourceTransactionCount);
        
        history.setTargetWalletId(targetWalletId);
        history.setTargetWalletName(targetWallet.getWalletName());
        history.setTargetCurrency(targetCurrency);
        history.setTargetBalanceBefore(targetBalanceBefore);
        history.setTargetBalanceAfter(targetWallet.getBalance());
        history.setTargetTransactionCountBefore(targetTransactionCountBefore);
        history.setMergedAt(mergeDate);
        history.setMergeDurationMs(System.currentTimeMillis() - startTime);
        
        WalletMergeHistory savedHistory = walletMergeHistoryRepository.save(history);

        // Tạo response
        MergeWalletResponse response = new MergeWalletResponse();
        response.setSuccess(true);
        response.setMessage("Gộp ví thành công");
        response.setTargetWalletId(targetWalletId);
        response.setTargetWalletName(targetWallet.getWalletName());
        response.setFinalBalance(targetWallet.getBalance());
        response.setFinalCurrency(targetCurrency);
        response.setMergedTransactions(sourceTransactionCount);
        response.setSourceWalletName(sourceWalletName);
        response.setWasDefaultTransferred(wasSourceDefault);
        response.setMergeHistoryId(savedHistory.getMergeId());
        response.setMergedAt(mergeDate);

        return response;
    }

    @Override
    @Transactional // Đảm bảo có @Transactional
    public DeleteWalletResponse deleteWallet(Long userId, Long walletId) {

        // 1. Tìm ví
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví"));

        // 2. Kiểm tra quyền sở hữu
        if (!isOwner(walletId, userId)) {
            throw new RuntimeException("Bạn không có quyền xóa ví này");
        }

        // 3. Kiểm tra nếu có giao dịch
        boolean hasTransactions = transactionRepository.existsByWallet_WalletId(walletId);
        if (hasTransactions) {
            throw new RuntimeException("Không thể xóa ví. Bạn phải xóa các giao dịch trong ví này trước.");
        }

        // 4. Lưu thông tin ví mặc định trước khi xóa
        boolean wasDefault = wallet.isDefault();
        
        // Kiểm tra nếu là ví mặc định
        if (wasDefault) {
            throw new RuntimeException("Không thể xóa ví mặc định.");
        }

        // 5. Xóa các thành viên liên quan
        List<WalletMember> members = walletMemberRepository.findByWallet_WalletId(walletId);
        int membersRemoved = members.size();
        walletMemberRepository.deleteAll(members);

        // 6. Xóa ví
        walletRepository.delete(wallet);

        // 7. Trả về thông tin
        DeleteWalletResponse response = new DeleteWalletResponse(
                wallet.getWalletId(),
                wallet.getWalletName(),
                wallet.getBalance(),
                wallet.getCurrencyCode()
        );
        response.setWasDefault(wasDefault);
        response.setMembersRemoved(membersRemoved);
        response.setTransactionsDeleted(0); // Không có transactions vì đã check ở trên
        
        return response;
    }


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
