package com.example.financeapp.wallet.service.impl;

import com.example.financeapp.fund.entity.Fund;
import com.example.financeapp.fund.repository.FundRepository;
import com.example.financeapp.transaction.entity.Transaction;
import com.example.financeapp.transaction.repository.TransactionRepository;
import com.example.financeapp.user.entity.User;
import com.example.financeapp.user.repository.UserRepository;
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
import com.example.financeapp.wallet.entity.WalletMember;
import com.example.financeapp.wallet.entity.WalletMember.WalletRole;
import com.example.financeapp.wallet.entity.WalletMergeHistory;
import com.example.financeapp.wallet.entity.WalletTransfer;
import com.example.financeapp.wallet.repository.CurrencyRepository;
import com.example.financeapp.wallet.repository.WalletMemberRepository;
import com.example.financeapp.wallet.repository.WalletMergeHistoryRepository;
import com.example.financeapp.wallet.repository.WalletRepository;
import com.example.financeapp.wallet.repository.WalletTransferRepository;
import com.example.financeapp.wallet.service.ExchangeRateService;
import com.example.financeapp.wallet.service.WalletService;
import com.example.financeapp.notification.service.NotificationService;
import com.example.financeapp.notification.entity.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WalletServiceImpl implements WalletService {

    @Autowired private WalletRepository walletRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CurrencyRepository currencyRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private WalletMemberRepository walletMemberRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private WalletMergeHistoryRepository walletMergeHistoryRepository;
    @Autowired private WalletTransferRepository walletTransferRepository;
    @Autowired private FundRepository fundRepository;

    @Autowired
    private ExchangeRateService exchangeRateService;

    // ---------------- CREATE WALLET ----------------
    @Override
    @Transactional
    public Wallet createWallet(Long userId, CreateWalletRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));



        // Hệ thống chỉ hỗ trợ VND
        String currencyCode = request.getCurrencyCode() != null ? request.getCurrencyCode().toUpperCase() : "VND";
        if (!currencyCode.equals("VND")) {
            throw new RuntimeException("Hệ thống chỉ hỗ trợ VND. Không thể tạo ví với loại tiền tệ: " + currencyCode);
        }
        if (!currencyRepository.existsById(currencyCode)) {
            throw new RuntimeException("Loại tiền tệ không hợp lệ: " + currencyCode);
        }

        if (walletRepository.existsByWalletNameAndUser_UserIdAndDeletedFalse(request.getWalletName(), userId)) {
            throw new RuntimeException("Bạn đã có ví tên \"" + request.getWalletName() + "\"");
        }

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setWalletName(request.getWalletName().trim());
        wallet.setCurrencyCode(currencyCode);
        BigDecimal initialBalance = BigDecimal.valueOf(request.getInitialBalance());
        wallet.setBalance(initialBalance);
        // KHÔNG lưu originalBalance khi tạo ví mới
        // Chỉ lưu khi chuyển đổi currency lần đầu để tránh lưu balance = 0
        wallet.setOriginalBalance(null);
        wallet.setOriginalCurrency(null);
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
        walletRepository.findByWalletIdAndUser_UserIdAndDeletedFalse(walletId, userId)
                .orElseThrow(() -> new RuntimeException("Ví không tồn tại"));

        walletRepository.unsetDefaultWallet(userId, walletId);
        walletRepository.setDefaultWallet(userId, walletId);
    }

    @Override
    public List<Wallet> getWalletsByUserId(Long userId) {
        return walletRepository.findByUser_UserIdAndDeletedFalse(userId);
    }

    @Override
    public Wallet getWalletDetails(Long userId, Long walletId) {
        if (!hasAccess(walletId, userId)) {
            throw new RuntimeException("Bạn không có quyền truy cập ví này");
        }

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví"));

        // Kiểm tra nếu ví đã bị xóa mềm
        if (wallet.isDeleted()) {
            throw new RuntimeException("Ví này đã bị xóa");
        }

        return wallet;
    }

    // ============= SHARED WALLET =============
    @Override
    public List<SharedWalletDTO> getAllAccessibleWallets(Long userId) {

        List<WalletMember> memberships = walletMemberRepository.findByUser_UserId(userId);
        List<SharedWalletDTO> result = new ArrayList<>();

        for (WalletMember membership : memberships) {

            Wallet wallet = membership.getWallet();

            // Bỏ qua ví đã bị xóa mềm
            if (wallet.isDeleted()) {
                continue;
            }

            WalletMember owner = walletMemberRepository
                    .findByWallet_WalletIdAndRole(wallet.getWalletId(), WalletRole.OWNER)
                    .orElse(null);

            long totalMembers = walletMemberRepository.countByWallet_WalletId(wallet.getWalletId());
            long transactionCount = transactionRepository.countByWallet_WalletId(wallet.getWalletId());

            SharedWalletDTO dto = new SharedWalletDTO();
            dto.setWalletId(wallet.getWalletId());
            dto.setWalletName(wallet.getWalletName());
            dto.setWalletType(wallet.getWalletType());
            dto.setCurrencyCode(wallet.getCurrencyCode());
            dto.setBalance(wallet.getBalance());
            dto.setDescription(wallet.getDescription());
            dto.setMyRole(membership.getRole().toString());
            dto.setTotalMembers((int) totalMembers);
            dto.setTransactionCount((int) transactionCount);
            dto.setDefault(wallet.isDefault());
            dto.setCreatedAt(wallet.getCreatedAt());
            dto.setUpdatedAt(wallet.getUpdatedAt());

            if (owner != null) {
                dto.setOwnerId(owner.getUser().getUserId());
                dto.setOwnerName(owner.getUser().getFullName());
                dto.setOwnerEmail(owner.getUser().getEmail());
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

        // Kiểm tra nếu ví đã bị xóa mềm
        if (wallet.isDeleted()) {
            throw new RuntimeException("Ví này đã bị xóa");
        }

        if (!isOwner(walletId, ownerId)) {
            throw new RuntimeException("Chỉ chủ sở hữu mới có thể chia sẻ ví");
        }

        User memberUser = userRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + memberEmail));

        if (memberUser.getUserId().equals(ownerId)) {
            throw new RuntimeException("Không thể chia sẻ ví với chính bạn");
        }

        // Kiểm tra xem member đã tồn tại chưa (bao gồm cả đã bị xóa mềm)
        Optional<WalletMember> existingMemberOpt = walletMemberRepository.findByWallet_WalletIdAndUser_UserId(walletId, memberUser.getUserId());

        WalletMember saved;
        if (existingMemberOpt.isPresent()) {
            WalletMember existingMember = existingMemberOpt.get();
            // Nếu member đã tồn tại và chưa bị xóa mềm
            if (!existingMember.isDeleted()) {
                throw new RuntimeException("Người dùng này đã là thành viên của ví");
            }
            // Nếu member đã bị xóa mềm, restore lại
            existingMember.setDeleted(false);
            existingMember.setDeletedAt(null);
            // Reset role về VIEW khi restore
            existingMember.setRole(WalletRole.VIEW);
            saved = walletMemberRepository.save(existingMember);
        } else {
            // Tạo member mới
            // Luôn tạo với role VIEW (Viewer) khi mời lần đầu, cả ví cá nhân và ví nhóm
            WalletRole defaultRole = WalletRole.VIEW;
            WalletMember newMember = new WalletMember(wallet, memberUser, defaultRole);
            saved = walletMemberRepository.save(newMember);
        }

        // Tạo thông báo cho người được mời - luôn là "Bạn có thể xem ví này"
        try {
            User owner = userRepository.findById(ownerId).orElse(null);
            String ownerEmail = owner != null && owner.getEmail() != null ? owner.getEmail() : "chủ ví";
            String walletName = wallet.getWalletName() != null ? wallet.getWalletName() : "ví";

            notificationService.createUserNotification(
                    memberUser.getUserId(),
                    Notification.NotificationType.WALLET_INVITED,
                    "Bạn đã được mời vào ví",
                    String.format("%s đã mời bạn tham gia ví \"%s\". Bạn có thể xem ví này.", ownerEmail, walletName),
                    walletId,
                    "WALLET"
            );
        } catch (Exception e) {
            // Log lỗi nhưng không throw để không ảnh hưởng đến việc chia sẻ ví
            System.err.println("Lỗi khi tạo thông báo cho người được mời: " + e.getMessage());
        }

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

        // Kiểm tra nếu member đã bị xóa mềm
        if (member.isDeleted()) {
            throw new RuntimeException("Thành viên này đã bị xóa khỏi ví");
        }

        // Lấy thông tin trước khi xóa để tạo notification
        Wallet wallet = member.getWallet();
        User removedUser = member.getUser();
        String walletName = wallet.getWalletName() != null ? wallet.getWalletName() : "ví";

        // Tìm owner của ví để lấy thông tin chủ ví
        WalletMember ownerMember = walletMemberRepository.findByWallet_WalletIdAndRole(walletId, WalletRole.OWNER)
                .stream()
                .findFirst()
                .orElse(null);

        String ownerEmail = "chủ ví";
        if (ownerMember != null && ownerMember.getUser() != null) {
            User owner = ownerMember.getUser();
            ownerEmail = owner.getEmail() != null ? owner.getEmail() : "chủ ví";
        }

        // XÓA MỀM: Đánh dấu deleted = true thay vì xóa cứng
        // Điều này giúp giữ lại lịch sử giao dịch liên quan đến thành viên này
        member.setDeleted(true);
        member.setDeletedAt(LocalDateTime.now());
        walletMemberRepository.save(member);

        // Tạo thông báo cho thành viên bị xóa
        if (removedUser != null) {
            try {
                notificationService.createUserNotification(
                        removedUser.getUserId(),
                        Notification.NotificationType.WALLET_MEMBER_REMOVED,
                        "Bạn đã bị xóa khỏi ví",
                        String.format("Bạn đã bị xóa khỏi ví \"%s\" bởi chủ ví %s.", walletName, ownerEmail),
                        walletId,
                        "WALLET"
                );
            } catch (Exception e) {
                // Log lỗi nhưng không throw để không ảnh hưởng đến việc xóa thành viên
                System.err.println("Lỗi khi tạo thông báo cho thành viên bị xóa: " + e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void updateMemberRole(Long walletId, Long operatorUserId, Long memberUserId, String role) {

        // Kiểm tra operator là owner
        if (!isOwner(walletId, operatorUserId)) {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ chủ sở hữu mới có thể thay đổi quyền thành viên");
        }

        // Không cho phép thay đổi quyền của chính mình (tránh mất quyền truy cập)
        if (operatorUserId.equals(memberUserId)) {
            throw new IllegalArgumentException("Không thể thay đổi quyền của chính bạn");
        }

        WalletMember target = walletMemberRepository
                .findByWallet_WalletIdAndUser_UserId(walletId, memberUserId)
                .orElseThrow(() -> new IllegalArgumentException("Thành viên không tồn tại trong ví"));

        WalletMember.WalletRole newRole;
        try {
            newRole = WalletMember.WalletRole.valueOf(role.toUpperCase());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Vai trò không hợp lệ. Giá trị hợp lệ: OWNER, MEMBER, VIEW");
        }

        // Nếu target hiện là OWNER và yêu cầu hạ quyền (không phải thông qua transfer ownership), chặn lại
        if (target.getRole() == WalletMember.WalletRole.OWNER && newRole != WalletMember.WalletRole.OWNER) {
            throw new IllegalArgumentException("Không thể hạ quyền OWNER trực tiếp. Hãy chuyển OWNER cho thành viên khác nếu cần.");
        }

        if (newRole == WalletMember.WalletRole.OWNER) {
            // Promote: hạ owner cũ xuống MEMBER, rồi nâng target lên OWNER
            walletMemberRepository.findByWallet_WalletIdAndRole(walletId, WalletMember.WalletRole.OWNER)
                    .ifPresent(oldOwner -> {
                        if (!oldOwner.getUser().getUserId().equals(target.getUser().getUserId())) {
                            oldOwner.setRole(WalletMember.WalletRole.MEMBER);
                            walletMemberRepository.save(oldOwner);
                        }
                    });

            target.setRole(WalletMember.WalletRole.OWNER);
            walletMemberRepository.save(target);
        } else {
            // Đặt thành MEMBER hoặc VIEW
            WalletRole oldRole = target.getRole();
            target.setRole(newRole);
            walletMemberRepository.save(target);

            // Tạo thông báo khi thay đổi quyền
            try {
                Wallet wallet = target.getWallet();
                String walletName = wallet.getWalletName() != null ? wallet.getWalletName() : "ví";
                User owner = wallet.getUser();
                String ownerEmail = owner != null && owner.getEmail() != null ? owner.getEmail() : "chủ ví";

                if (oldRole == WalletRole.VIEW && newRole == WalletRole.MEMBER) {
                    // Nâng quyền từ VIEW lên MEMBER
                    notificationService.createUserNotification(
                            target.getUser().getUserId(),
                            Notification.NotificationType.WALLET_ROLE_UPDATED,
                            "Quyền truy cập ví đã được nâng cấp",
                            String.format("%s đã nâng quyền của bạn trong ví \"%s\" lên thành viên. Bạn có thể xem và quản lý ví này.", ownerEmail, walletName),
                            walletId,
                            "WALLET"
                    );
                } else if (oldRole == WalletRole.MEMBER && newRole == WalletRole.VIEW) {
                    // Hạ quyền từ MEMBER xuống VIEW
                    notificationService.createUserNotification(
                            target.getUser().getUserId(),
                            Notification.NotificationType.WALLET_ROLE_UPDATED,
                            "Quyền truy cập ví đã được thay đổi",
                            String.format("%s đã thay đổi quyền của bạn trong ví \"%s\" xuống người xem. Bạn chỉ có thể xem ví này.", ownerEmail, walletName),
                            walletId,
                            "WALLET"
                    );
                }
            } catch (Exception e) {
                // Log lỗi nhưng không throw để không ảnh hưởng đến việc cập nhật role
                System.err.println("Lỗi khi tạo thông báo thay đổi quyền: " + e.getMessage());
            }
        }
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

        // Kiểm tra nếu ví đã bị xóa mềm
        if (wallet.isDeleted()) {
            throw new RuntimeException("Ví này đã bị xóa");
        }

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

        // Cập nhật tiền tệ và chuyển đổi số dư nếu currency thay đổi
        if (request.getCurrencyCode() != null) {
            String newCurrency = request.getCurrencyCode().toUpperCase();

            // Hệ thống chỉ hỗ trợ VND
            if (!newCurrency.equals("VND")) {
                throw new RuntimeException("Hệ thống chỉ hỗ trợ VND. Không thể đổi sang loại tiền tệ: " + newCurrency);
            }

            if (!currencyRepository.existsById(newCurrency)) {
                throw new RuntimeException("Mã tiền tệ không tồn tại");
            }

            String oldCurrency = wallet.getCurrencyCode();

            // Nếu currency thay đổi, chuyển đổi số dư và transactions
            if (!oldCurrency.equals(newCurrency)) {
                BigDecimal convertedBalance;

                // Lưu currency gốc và số dư gốc để tracking (chỉ lưu lần đầu tiên chuyển đổi currency)
                if (wallet.getOriginalCurrency() == null) {
                    wallet.setOriginalCurrency(oldCurrency);
                    wallet.setOriginalBalance(wallet.getBalance());
                }

                // Nếu đang chuyển về currency gốc
                if (newCurrency.equals(wallet.getOriginalCurrency())) {
                    // Tính toán số dư dựa trên số dư hiện tại và tỷ giá ngược
                    BigDecimal reverseRate = exchangeRateService.getExchangeRate(oldCurrency, newCurrency);
                    BigDecimal calculatedFromCurrent = wallet.getBalance().multiply(reverseRate);

                    // Tính toán số dư dựa trên originalBalance và tỷ giá (nếu không có giao dịch)
                    BigDecimal calculatedFromOriginal = null;
                    if (wallet.getOriginalBalance() != null) {
                        // Tính xem originalBalance đã được chuyển đổi sang oldCurrency như thế nào
                        // Nếu originalCurrency == newCurrency (currency gốc), thì dùng trực tiếp
                        if (wallet.getOriginalCurrency().equals(newCurrency)) {
                            calculatedFromOriginal = wallet.getOriginalBalance();
                        }
                    }

                    // Xác định số chữ số thập phân thực tế của originalBalance
                    int originalScale = 8; // Mặc định 8 chữ số
                    if (wallet.getOriginalBalance() != null) {
                        // Lấy số chữ số thập phân thực tế (loại bỏ số 0 ở cuối)
                        String originalStr = wallet.getOriginalBalance().stripTrailingZeros().toPlainString();
                        int dotIndex = originalStr.indexOf('.');
                        if (dotIndex >= 0) {
                            originalScale = originalStr.length() - dotIndex - 1;
                        } else {
                            originalScale = 0; // Số nguyên
                        }
                        // Giới hạn tối đa 8 chữ số (theo scale của database)
                        originalScale = Math.min(originalScale, 8);
                    }

                    // Nếu số dư tính từ current balance gần với originalBalance (sai số < 0.01),
                    // có nghĩa là không có giao dịch, dùng originalBalance để tránh sai số tích lũy
                    if (calculatedFromOriginal != null) {
                        // Làm tròn calculatedFromCurrent về số chữ số thập phân của originalBalance
                        calculatedFromCurrent = calculatedFromCurrent.setScale(originalScale, RoundingMode.HALF_UP);
                        BigDecimal difference = calculatedFromCurrent.subtract(calculatedFromOriginal).abs();
                        if (difference.compareTo(new BigDecimal("0.01")) < 0) {
                            // Không có giao dịch, dùng originalBalance để đảm bảo tính đối xứng
                            convertedBalance = calculatedFromOriginal;
                        } else {
                            // Có giao dịch, dùng số dư tính từ current balance (đã làm tròn về originalScale)
                            convertedBalance = calculatedFromCurrent;
                            // Cập nhật originalBalance để lần sau có thể dùng
                            wallet.setOriginalBalance(convertedBalance);
                        }
                    } else {
                        // Không có originalBalance, dùng số dư tính từ current balance
                        // Làm tròn về số chữ số thập phân hợp lý (3 chữ số cho VND, 8 chữ số cho USD)
                        int targetScale = newCurrency.equals("VND") ? 3 : 8;
                        convertedBalance = calculatedFromCurrent.setScale(targetScale, RoundingMode.HALF_UP);
                        wallet.setOriginalBalance(convertedBalance);
                    }
                    wallet.setBalance(convertedBalance);
                } else {
                    // Chuyển đổi từ currency hiện tại sang currency mới
                    convertedBalance = exchangeRateService.convertAmount(
                            wallet.getBalance(),
                            oldCurrency,
                            newCurrency
                    );
                    wallet.setBalance(convertedBalance);
                }

                // Chuyển đổi tất cả transactions (nếu có)
                List<Transaction> transactions = transactionRepository.findByWallet_WalletId(walletId);
                for (Transaction tx : transactions) {
                    // Xác định currency gốc của transaction
                    // Nếu transaction đã có originalCurrency (từ lần chuyển đổi trước), dùng nó
                    // Nếu chưa có, dùng oldCurrency (currency hiện tại của ví)
                    String txOriginalCurrency = tx.getOriginalCurrency() != null
                            ? tx.getOriginalCurrency()
                            : oldCurrency;

                    // Lưu thông tin gốc nếu chưa có
                    if (tx.getOriginalAmount() == null) {
                        tx.setOriginalAmount(tx.getAmount());
                        tx.setOriginalCurrency(oldCurrency);
                        txOriginalCurrency = oldCurrency;
                    }

                    // Chuyển đổi amount từ currency gốc sang currency mới
                    BigDecimal convertedAmount = exchangeRateService.convertAmount(
                            tx.getOriginalAmount(),
                            txOriginalCurrency,
                            newCurrency
                    );
                    tx.setAmount(convertedAmount);

                    // Lưu exchange rate từ currency gốc sang currency mới
                    BigDecimal rate = exchangeRateService.getExchangeRate(
                            txOriginalCurrency,
                            newCurrency
                    );
                    tx.setExchangeRate(rate);

                    transactionRepository.save(tx);
                }

                // Chuyển đổi tất cả WalletTransfers liên quan đến ví này (cả gửi và nhận)
                // 1. Transfers từ ví này (fromWallet)
                List<WalletTransfer> fromTransfers = walletTransferRepository.findByFromWallet_WalletIdOrderByTransferDateDesc(walletId);
                for (WalletTransfer transfer : fromTransfers) {
                    // Xác định currency gốc của transfer
                    String transferOriginalCurrency = transfer.getOriginalCurrency() != null
                            ? transfer.getOriginalCurrency()
                            : oldCurrency;

                    // Lưu thông tin gốc nếu chưa có
                    if (transfer.getOriginalAmount() == null) {
                        transfer.setOriginalAmount(transfer.getAmount());
                        transfer.setOriginalCurrency(oldCurrency);
                        transferOriginalCurrency = oldCurrency;
                    }

                    // Chuyển đổi amount từ currency gốc sang currency mới
                    BigDecimal convertedAmount = exchangeRateService.convertAmount(
                            transfer.getOriginalAmount(),
                            transferOriginalCurrency,
                            newCurrency
                    );
                    transfer.setAmount(convertedAmount);
                    transfer.setCurrencyCode(newCurrency);

                    // Lưu exchange rate từ currency gốc sang currency mới
                    BigDecimal rate = exchangeRateService.getExchangeRate(
                            transferOriginalCurrency,
                            newCurrency
                    );
                    transfer.setExchangeRate(rate);

                    // Chuyển đổi balance tracking
                    if (transfer.getFromBalanceBefore() != null) {
                        BigDecimal convertedBefore = exchangeRateService.convertAmount(
                                transfer.getFromBalanceBefore(),
                                oldCurrency,
                                newCurrency
                        );
                        transfer.setFromBalanceBefore(convertedBefore);
                    }
                    if (transfer.getFromBalanceAfter() != null) {
                        BigDecimal convertedAfter = exchangeRateService.convertAmount(
                                transfer.getFromBalanceAfter(),
                                oldCurrency,
                                newCurrency
                        );
                        transfer.setFromBalanceAfter(convertedAfter);
                    }

                    walletTransferRepository.save(transfer);
                }

                // 2. Transfers đến ví này (toWallet)
                // Khi ví nhận đổi currency, chỉ chuyển đổi balance tracking của ví nhận
                // Amount và currency_code của transfer giữ nguyên (theo currency của ví gửi)
                List<WalletTransfer> toTransfers = walletTransferRepository.findByToWallet_WalletIdOrderByTransferDateDesc(walletId);
                for (WalletTransfer transfer : toTransfers) {
                    // Chuyển đổi balance tracking của ví nhận (toWallet)
                    // Balance tracking được lưu theo currency của ví nhận tại thời điểm transfer
                    if (transfer.getToBalanceBefore() != null) {
                        BigDecimal convertedBefore = exchangeRateService.convertAmount(
                                transfer.getToBalanceBefore(),
                                oldCurrency, // Currency cũ của ví nhận
                                newCurrency  // Currency mới của ví nhận
                        );
                        transfer.setToBalanceBefore(convertedBefore);
                    }
                    if (transfer.getToBalanceAfter() != null) {
                        BigDecimal convertedAfter = exchangeRateService.convertAmount(
                                transfer.getToBalanceAfter(),
                                oldCurrency, // Currency cũ của ví nhận
                                newCurrency  // Currency mới của ví nhận
                        );
                        transfer.setToBalanceAfter(convertedAfter);
                    }

                    walletTransferRepository.save(transfer);
                }

                // Chuyển đổi tất cả Funds liên quan đến ví này
                List<Fund> funds = fundRepository.findByTargetWallet_WalletId(walletId);
                for (Fund fund : funds) {
                    // Chuyển đổi currentAmount
                    if (fund.getCurrentAmount() != null && fund.getCurrentAmount().compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal convertedCurrentAmount = exchangeRateService.convertAmount(
                                fund.getCurrentAmount(),
                                oldCurrency,
                                newCurrency
                        );
                        fund.setCurrentAmount(convertedCurrentAmount);
                    }

                    // Chuyển đổi targetAmount (nếu có)
                    if (fund.getTargetAmount() != null && fund.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal convertedTargetAmount = exchangeRateService.convertAmount(
                                fund.getTargetAmount(),
                                oldCurrency,
                                newCurrency
                        );
                        fund.setTargetAmount(convertedTargetAmount);
                    }

                    // Chuyển đổi amountPerPeriod (nếu có)
                    if (fund.getAmountPerPeriod() != null && fund.getAmountPerPeriod().compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal convertedAmountPerPeriod = exchangeRateService.convertAmount(
                                fund.getAmountPerPeriod(),
                                oldCurrency,
                                newCurrency
                        );
                        fund.setAmountPerPeriod(convertedAmountPerPeriod);
                    }

                    // Chuyển đổi autoDepositAmount (nếu có)
                    if (fund.getAutoDepositAmount() != null && fund.getAutoDepositAmount().compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal convertedAutoDepositAmount = exchangeRateService.convertAmount(
                                fund.getAutoDepositAmount(),
                                oldCurrency,
                                newCurrency
                        );
                        fund.setAutoDepositAmount(convertedAutoDepositAmount);
                    }

                    fundRepository.save(fund);
                }
            }

            wallet.setCurrencyCode(newCurrency);
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
                // Kiểm tra nếu ví là ví mặc định thì không cho chuyển đổi
                if (wallet.isDefault()) {
                    throw new RuntimeException("Không thể chuyển đổi ví mặc định sang ví nhóm. Vui lòng bỏ ví mặc định trước.");
                }

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

        // Kiểm tra nếu member đã bị xóa mềm
        if (member.isDeleted()) {
            throw new RuntimeException("Bạn đã rời khỏi ví này rồi");
        }

        if (member.getRole() == WalletRole.OWNER) {
            throw new RuntimeException("Chủ sở hữu không thể tự rời ví");
        }

        // Lấy thông tin trước khi xóa để tạo notification
        Wallet wallet = member.getWallet();
        User leavingUser = member.getUser();
        String leavingUserEmail = leavingUser.getEmail() != null ? leavingUser.getEmail() : "thành viên";
        String walletName = wallet.getWalletName() != null ? wallet.getWalletName() : "ví";

        // Tìm owner của ví để gửi thông báo
        WalletMember ownerMember = walletMemberRepository.findByWallet_WalletIdAndRole(walletId, WalletRole.OWNER)
                .stream()
                .findFirst()
                .orElse(null);

        // XÓA MỀM: Đánh dấu deleted = true thay vì xóa cứng
        // Điều này giúp giữ lại lịch sử giao dịch liên quan đến thành viên này
        member.setDeleted(true);
        member.setDeletedAt(LocalDateTime.now());
        walletMemberRepository.save(member);

        // Tạo thông báo cho chủ ví
        if (ownerMember != null && ownerMember.getUser() != null) {
            try {
                notificationService.createUserNotification(
                        ownerMember.getUser().getUserId(),
                        Notification.NotificationType.WALLET_MEMBER_LEFT,
                        "Thành viên đã rời khỏi ví",
                        String.format("%s đã rời khỏi ví \"%s\".", leavingUserEmail, walletName),
                        walletId,
                        "WALLET"
                );
            } catch (Exception e) {
                // Log lỗi nhưng không throw để không ảnh hưởng đến việc rời ví
                System.err.println("Lỗi khi tạo thông báo cho chủ ví: " + e.getMessage());
            }
        }
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
        if (sourceWallet.isDeleted()) {
            throw new RuntimeException("Ví nguồn đã bị xóa");
        }

        Wallet targetWallet = walletRepository.findById(targetWalletId)
                .orElseThrow(() -> new RuntimeException("Ví đích không tồn tại"));
        if (targetWallet.isDeleted()) {
            throw new RuntimeException("Ví đích đã bị xóa");
        }

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
    public MergeWalletResponse mergeWallets(Long userId, Long sourceWalletId, Long targetWalletId, String targetCurrency, Boolean setTargetAsDefault) {
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
        if (sourceWallet.isDeleted()) {
            throw new RuntimeException("Ví nguồn đã bị xóa");
        }

        Wallet targetWallet = walletRepository.findByIdWithLock(targetWalletId)
                .orElseThrow(() -> new RuntimeException("Ví đích không tồn tại"));
        if (targetWallet.isDeleted()) {
            throw new RuntimeException("Ví đích đã bị xóa");
        }

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

        // Lưu currency gốc của target wallet TRƯỚC KHI thay đổi (quan trọng!)
        String originalTargetCurrency = targetWallet.getCurrencyCode();

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
        if (!originalTargetCurrency.equals(targetCurrency)) {
            targetBalanceConverted = exchangeRateService.convertAmount(
                    targetBalanceBefore,
                    originalTargetCurrency,
                    targetCurrency
            );
        }

        // Cập nhật target wallet
        targetWallet.setCurrencyCode(targetCurrency);
        targetWallet.setBalance(sourceBalanceConverted.add(targetBalanceConverted));

        // Xử lý ví mặc định
        if (setTargetAsDefault != null) {
            // Nếu có chỉ định rõ ràng
            if (Boolean.TRUE.equals(setTargetAsDefault)) {
                // Đặt ví đích làm ví mặc định
                walletRepository.unsetDefaultWallet(userId, targetWalletId);
                targetWallet.setDefault(true);
            } else {
                // Bỏ ví mặc định (không đặt ví đích làm ví mặc định)
                targetWallet.setDefault(false);
            }
        } else {
            // Nếu không có chỉ định, tự động chuyển từ source nếu source là default
            if (wasSourceDefault) {
                walletRepository.unsetDefaultWallet(userId, targetWalletId);
                targetWallet.setDefault(true);
            }
        }

        walletRepository.save(targetWallet);

        // Chuyển tất cả transactions từ source sang target
        List<Transaction> sourceTransactions = transactionRepository.findByWallet_WalletId(sourceWalletId);
        LocalDateTime mergeDate = LocalDateTime.now();

        // Lưu currency gốc của source wallet trước khi thay đổi
        String originalSourceCurrency = sourceWallet.getCurrencyCode();

        for (Transaction tx : sourceTransactions) {
            // Xác định currency gốc của transaction
            // Nếu transaction đã có originalCurrency (từ lần chuyển đổi trước), dùng nó
            // Nếu chưa có, dùng currency hiện tại của source wallet
            String txOriginalCurrency = tx.getOriginalCurrency() != null
                    ? tx.getOriginalCurrency()
                    : originalSourceCurrency;

            // Xác định amount gốc của transaction
            // Nếu transaction đã có originalAmount (từ lần chuyển đổi trước), dùng nó
            // Nếu chưa có, dùng amount hiện tại
            BigDecimal txOriginalAmount = tx.getOriginalAmount() != null
                    ? tx.getOriginalAmount()
                    : tx.getAmount();

            // Lưu thông tin gốc nếu chưa có
            if (tx.getOriginalAmount() == null) {
                tx.setOriginalAmount(txOriginalAmount);
                tx.setOriginalCurrency(txOriginalCurrency);
            }

            // Nếu currency gốc khác với target currency, cần chuyển đổi
            if (!txOriginalCurrency.equals(targetCurrency)) {
                // Chuyển đổi từ currency gốc sang target currency
                BigDecimal convertedAmount = exchangeRateService.convertAmount(
                        txOriginalAmount,
                        txOriginalCurrency,
                        targetCurrency
                );
                tx.setAmount(convertedAmount);

                // Lưu exchange rate từ currency gốc sang target currency
                BigDecimal rate = exchangeRateService.getExchangeRate(
                        txOriginalCurrency,
                        targetCurrency
                );
                tx.setExchangeRate(rate);
            } else {
                // Nếu currency gốc trùng với target currency, không cần chuyển đổi
                // Nhưng vẫn cần đảm bảo amount = originalAmount
                tx.setAmount(txOriginalAmount);
                tx.setExchangeRate(BigDecimal.ONE);
            }

            tx.setWallet(targetWallet);
            tx.setMergeDate(mergeDate);
            transactionRepository.save(tx);
        }

        // Chuyển đổi tất cả transactions của target wallet nếu currency khác targetCurrency
        // originalTargetCurrency đã được lấy ở trên (trước khi cập nhật wallet)

        if (!originalTargetCurrency.equals(targetCurrency)) {
            List<Transaction> targetTransactions = transactionRepository.findByWallet_WalletId(targetWalletId);

            for (Transaction tx : targetTransactions) {
                // Xác định currency gốc của transaction
                // Nếu transaction đã có originalCurrency (từ lần chuyển đổi trước), dùng nó
                // Nếu chưa có, dùng currency hiện tại của target wallet
                String txOriginalCurrency = tx.getOriginalCurrency() != null
                        ? tx.getOriginalCurrency()
                        : originalTargetCurrency;

                // Xác định amount gốc của transaction
                // Nếu transaction đã có originalAmount (từ lần chuyển đổi trước), dùng nó
                // Nếu chưa có, dùng amount hiện tại
                BigDecimal txOriginalAmount = tx.getOriginalAmount() != null
                        ? tx.getOriginalAmount()
                        : tx.getAmount();

                // Lưu thông tin gốc nếu chưa có
                if (tx.getOriginalAmount() == null) {
                    tx.setOriginalAmount(txOriginalAmount);
                    tx.setOriginalCurrency(txOriginalCurrency);
                }

                // Nếu currency gốc khác với target currency, cần chuyển đổi
                if (!txOriginalCurrency.equals(targetCurrency)) {
                    // Chuyển đổi từ currency gốc sang target currency
                    BigDecimal convertedAmount = exchangeRateService.convertAmount(
                            txOriginalAmount,
                            txOriginalCurrency,
                            targetCurrency
                    );
                    tx.setAmount(convertedAmount);

                    // Lưu exchange rate từ currency gốc sang target currency
                    BigDecimal rate = exchangeRateService.getExchangeRate(
                            txOriginalCurrency,
                            targetCurrency
                    );
                    tx.setExchangeRate(rate);
                } else {
                    // Nếu currency gốc trùng với target currency, không cần chuyển đổi
                    // Nhưng vẫn cần đảm bảo amount = originalAmount
                    tx.setAmount(txOriginalAmount);
                    tx.setExchangeRate(BigDecimal.ONE);
                }

                // Đánh dấu transaction này cũng bị ảnh hưởng bởi merge
                tx.setMergeDate(mergeDate);
                transactionRepository.save(tx);
            }
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

        // Cập nhật source wallet transfers thay vì xóa để giữ lại lịch sử
        // Lấy tất cả transfers liên quan đến source wallet
        List<WalletTransfer> sourceTransfers = walletTransferRepository.findByWalletId(sourceWalletId);

        for (WalletTransfer transfer : sourceTransfers) {
            boolean fromIsSource = transfer.getFromWallet().getWalletId().equals(sourceWalletId);
            boolean toIsSource = transfer.getToWallet().getWalletId().equals(sourceWalletId);

            // Nếu cả fromWallet và toWallet đều là source wallet (không nên xảy ra), xóa transfer
            if (fromIsSource && toIsSource) {
                walletTransferRepository.delete(transfer);
                continue;
            }

            // Update fromWallet nếu là source wallet
            if (fromIsSource) {
                transfer.setFromWallet(targetWallet);
            }

            // Update toWallet nếu là source wallet
            if (toIsSource) {
                transfer.setToWallet(targetWallet);
            }

            // Nếu sau khi update, cả fromWallet và toWallet đều là target wallet
            // (tức là cả 2 ví đều được gộp vào target), xóa transfer vì không còn ý nghĩa
            if (transfer.getFromWallet().getWalletId().equals(targetWalletId) &&
                    transfer.getToWallet().getWalletId().equals(targetWalletId)) {
                walletTransferRepository.delete(transfer);
                continue;
            }

            // Lưu transfer đã được update
            walletTransferRepository.save(transfer);
        }

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

        // Gửi thông báo cho tất cả thành viên của cả 2 ví (trước khi gộp)
        try {
            // Lấy danh sách member (bao gồm cả owner) của source và target trước khi gộp
            List<WalletMember> allSourceMembers = sourceMembers;
            List<WalletMember> allTargetMembers = walletMemberRepository.findByWallet_WalletId(targetWalletId);

            // Gộp và loại bỏ trùng lặp theo userId
            List<Long> notifiedUserIds = new ArrayList<>();

            for (WalletMember member : allSourceMembers) {
                Long memberUserId = member.getUser().getUserId();
                if (!notifiedUserIds.contains(memberUserId)) {
                    notifiedUserIds.add(memberUserId);
                }
            }

            for (WalletMember member : allTargetMembers) {
                Long memberUserId = member.getUser().getUserId();
                if (!notifiedUserIds.contains(memberUserId)) {
                    notifiedUserIds.add(memberUserId);
                }
            }

            // Nội dung thông báo
            String title = "Ví đã được gộp";
            String ownerEmail = "chủ ví";
            try {
                User ownerUser = userRepository.findById(userId).orElse(null);
                if (ownerUser != null && ownerUser.getEmail() != null && !ownerUser.getEmail().isBlank()) {
                    ownerEmail = ownerUser.getEmail();
                }
            } catch (Exception ignore) {}

            String message = String.format(
                    "Ví \"%s\" đã được chủ ví \"%s\" gộp vào ví \"%s\". Số dư và lịch sử giao dịch của ví cũ đã được chuyển sang ví mới.",
                    sourceWalletName,
                    ownerEmail,
                    targetWallet.getWalletName()
            );

            for (Long memberUserId : notifiedUserIds) {
                notificationService.createUserNotification(
                        memberUserId,
                        Notification.NotificationType.WALLET_MERGED,
                        title,
                        message,
                        targetWalletId,
                        "WALLET"
                );
            }
        } catch (Exception ex) {
            // Không để lỗi thông báo ảnh hưởng tới quá trình merge ví
            ex.printStackTrace();
        }

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

    // ==============================
    // XÓA VÍ (SOFT DELETE)
    // ==============================
    @Override
    @Transactional
    public DeleteWalletResponse deleteWallet(Long userId, Long walletId) {

        // 1. Tìm ví
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví"));

        // 2. Kiểm tra nếu đã bị xóa mềm
        if (wallet.isDeleted()) {
            throw new RuntimeException("Ví này đã bị xóa");
        }

        // 3. Kiểm tra quyền sở hữu
        if (!isOwner(walletId, userId)) {
            throw new RuntimeException("Bạn không có quyền xóa ví này");
        }

        // 4. Lưu thông tin ví mặc định trước khi xóa
        boolean wasDefault = wallet.isDefault();

        // Kiểm tra nếu là ví mặc định
        if (wasDefault) {
            throw new RuntimeException("Không thể xóa ví mặc định.");
        }

        // 5. Lưu thông tin thành viên trước khi xóa (để trả về response và gửi thông báo)
        List<WalletMember> members = walletMemberRepository.findByWallet_WalletId(walletId);
        int membersRemoved = members.size();

        // Lấy thông tin chủ ví để hiển thị trong thông báo (sử dụng email)
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chủ ví"));
        String ownerEmail = owner.getEmail() != null ? owner.getEmail() : "chủ ví";
        String walletName = wallet.getWalletName() != null ? wallet.getWalletName() : "ví";

        // 6. XÓA MỀM: Chỉ đánh dấu deleted = true, không xóa khỏi database
        wallet.setDeleted(true);
        walletRepository.save(wallet);

        // 7. Gửi thông báo cho tất cả thành viên (trừ chủ ví)
        for (WalletMember member : members) {
            // Bỏ qua chủ ví (không gửi thông báo cho chính mình)
            if (member.getUser().getUserId().equals(userId)) {
                continue;
            }

            // Bỏ qua thành viên đã bị xóa mềm
            if (member.isDeleted()) {
                continue;
            }

            try {
                notificationService.createUserNotification(
                        member.getUser().getUserId(),
                        Notification.NotificationType.WALLET_DELETED,
                        "Ví đã bị xóa",
                        String.format("Ví \"%s\" đã bị xóa bởi chủ ví %s.", walletName, ownerEmail),
                        walletId,
                        "WALLET"
                );
            } catch (Exception e) {
                // Log lỗi nhưng không throw để không ảnh hưởng đến việc xóa ví
                System.err.println("Lỗi khi tạo thông báo cho thành viên khi xóa ví: " + e.getMessage());
            }
        }

        // 8. Trả về thông tin
        DeleteWalletResponse response = new DeleteWalletResponse(
                wallet.getWalletId(),
                wallet.getWalletName(),
                wallet.getBalance(),
                wallet.getCurrencyCode()
        );
        response.setWasDefault(wasDefault);
        response.setMembersRemoved(membersRemoved);
        response.setTransactionsDeleted(0); // Soft delete không xóa transactions

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
        if (fromWallet.isDeleted()) {
            throw new RuntimeException("Ví nguồn đã bị xóa");
        }

        Wallet toWallet = walletRepository.findByIdWithLock(request.getToWalletId())
                .orElseThrow(() -> new RuntimeException("Ví đích không tồn tại"));
        if (toWallet.isDeleted()) {
            throw new RuntimeException("Ví đích đã bị xóa");
        }

        if (!hasAccess(request.getFromWalletId(), userId))
            throw new RuntimeException("Bạn không có quyền ví nguồn");

        if (!hasAccess(request.getToWalletId(), userId))
            throw new RuntimeException("Bạn không có quyền ví đích");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // Xác định currency của số tiền nhập vào (theo ví gửi nếu không có targetCurrencyCode)
        String sourceCurrency = request.getTargetCurrencyCode() != null
                ? request.getTargetCurrencyCode()
                : fromWallet.getCurrencyCode();

        // Số tiền nhập vào (theo source currency - currency của ví gửi)
        BigDecimal sourceAmount = request.getAmount();

        // Kiểm tra số dư ví nguồn (theo source currency)
        if (fromWallet.getBalance().compareTo(sourceAmount) < 0)
            throw new RuntimeException("Số dư ví nguồn không đủ");

        // Chuyển đổi số tiền từ source currency sang target currency để cộng vào ví nhận
        BigDecimal targetAmount = sourceAmount;
        if (!fromWallet.getCurrencyCode().equals(toWallet.getCurrencyCode())) {
            targetAmount = exchangeRateService.convertAmount(
                    sourceAmount,
                    fromWallet.getCurrencyCode(),
                    toWallet.getCurrencyCode()
            );
        }

        BigDecimal fromBefore = fromWallet.getBalance();
        BigDecimal toBefore = toWallet.getBalance();

        long sourceMembers = walletMemberRepository.countByWallet_WalletId(fromWallet.getWalletId());
        long targetMembers = walletMemberRepository.countByWallet_WalletId(toWallet.getWalletId());

        boolean sourceShared = sourceMembers > 1;
        boolean targetShared = targetMembers > 1;

        LocalDateTime time = LocalDateTime.now();

        // Trừ số tiền từ ví nguồn (theo source currency)
        fromWallet.setBalance(fromBefore.subtract(sourceAmount));
        walletRepository.save(fromWallet);

        // Cộng số tiền vào ví nhận (theo target currency - đã chuyển đổi)
        toWallet.setBalance(toBefore.add(targetAmount));
        walletRepository.save(toWallet);

        WalletTransfer transfer = new WalletTransfer();
        transfer.setFromWallet(fromWallet);
        transfer.setToWallet(toWallet);
        transfer.setAmount(sourceAmount); // Lưu số tiền gốc (theo source currency - currency của ví gửi)
        transfer.setCurrencyCode(sourceCurrency); // Lưu currency của số tiền gốc
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
        response.setAmount(sourceAmount); // Số tiền gốc (theo source currency - currency của ví gửi)
        response.setCurrencyCode(sourceCurrency); // Currency của số tiền gốc
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

    // ---------------- GET ALL TRANSFERS ----------------
    @Override
    public List<WalletTransfer> getAllTransfers(Long userId) {
        return walletTransferRepository.findByUser_UserIdOrderByTransferDateDesc(userId);
    }

    // ---------------- UPDATE TRANSFER ----------------
    @Override
    @Transactional
    public WalletTransfer updateTransfer(Long userId, Long transferId, UpdateTransferRequest request) {
        // 1. Tìm transfer với user được fetch để tránh lazy loading exception
        WalletTransfer transfer = walletTransferRepository.findByIdWithUser(transferId)
                .orElseThrow(() -> new RuntimeException("Giao dịch chuyển tiền không tồn tại"));

        // 2. Kiểm tra quyền sở hữu
        if (transfer.getUser() == null || !transfer.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa giao dịch này");
        }

        // 3. Cập nhật chỉ note
        if (request.getNote() != null) {
            transfer.setNote(request.getNote().trim().isEmpty() ? null : request.getNote().trim());
        } else {
            transfer.setNote(null);
        }

        // 4. Lưu và trả về
        return walletTransferRepository.save(transfer);
    }

    // ---------------- DELETE TRANSFER ----------------
    @Override
    @Transactional
    public void deleteTransfer(Long userId, Long transferId) {
        // 1. Tìm transfer với tất cả relationships
        WalletTransfer transfer = walletTransferRepository.findByIdForDelete(transferId)
                .orElseThrow(() -> new RuntimeException("Giao dịch chuyển tiền không tồn tại"));

        // 2. Kiểm tra quyền sở hữu
        if (transfer.getUser() == null || !transfer.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa giao dịch này");
        }

        // 3. Lấy wallets với PESSIMISTIC LOCK để tránh race condition
        Wallet fromWallet = walletRepository.findByIdWithLock(transfer.getFromWallet().getWalletId())
                .orElseThrow(() -> new RuntimeException("Ví gửi không tồn tại"));
        if (fromWallet.isDeleted()) {
            throw new RuntimeException("Ví gửi đã bị xóa");
        }
        Wallet toWallet = walletRepository.findByIdWithLock(transfer.getToWallet().getWalletId())
                .orElseThrow(() -> new RuntimeException("Ví nhận không tồn tại"));
        if (toWallet.isDeleted()) {
            throw new RuntimeException("Ví nhận đã bị xóa");
        }

        // 4. Tính toán số tiền cần revert
        // Số tiền gốc (theo currency của ví gửi)
        BigDecimal originalAmount = transfer.getAmount();

        // Số tiền đã được cộng vào ví nhận (tính từ balance tracking)
        // Sử dụng toBalanceAfter - toBalanceBefore để có số tiền chính xác đã được cộng vào
        BigDecimal targetAmountAdded = transfer.getToBalanceAfter().subtract(transfer.getToBalanceBefore());

        // 5. Revert balance
        // Ví gửi: cộng lại số tiền (theo currency của ví gửi)
        BigDecimal newFromBalance = fromWallet.getBalance().add(originalAmount);

        // Ví nhận: trừ số tiền đã được cộng vào (theo currency của ví nhận)
        BigDecimal newToBalance = toWallet.getBalance().subtract(targetAmountAdded);

        // 6. Kiểm tra ví nhận không được âm
        if (newToBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Không thể xóa giao dịch vì ví âm tiền");
        }

        // 7. Cập nhật số dư
        fromWallet.setBalance(newFromBalance);
        walletRepository.save(fromWallet);

        toWallet.setBalance(newToBalance);
        walletRepository.save(toWallet);

        // 8. Xóa transfer
        walletTransferRepository.delete(transfer);
    }

    @Override
    public List<WalletTransactionHistoryDTO> getWalletTransactions(Long userId, Long walletId) {
        assertWalletAccess(walletId, userId);

        List<Transaction> transactions = transactionRepository.findDetailedByWalletId(walletId);
        return transactions.stream()
                .map(this::mapTransactionHistory)
                .collect(Collectors.toList());
    }

    @Override
    public List<WalletTransferHistoryDTO> getWalletTransfers(Long userId, Long walletId) {
        assertWalletAccess(walletId, userId);

        List<WalletTransfer> transfers = walletTransferRepository.findByWalletId(walletId);
        return transfers.stream()
                .map(transfer -> mapTransferHistory(transfer, walletId))
                .collect(Collectors.toList());
    }

    // ---------------- HELPER ----------------
    private void assertWalletAccess(Long walletId, Long userId) {
        if (!hasAccess(walletId, userId)) {
            throw new RuntimeException("Bạn không có quyền truy cập ví này");
        }
    }

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

    private WalletTransactionHistoryDTO mapTransactionHistory(Transaction transaction) {
        WalletTransactionHistoryDTO dto = new WalletTransactionHistoryDTO();
        dto.setTransactionId(transaction.getTransactionId());
        dto.setAmount(transaction.getAmount());
        dto.setOriginalAmount(transaction.getOriginalAmount());
        dto.setCurrencyCode(transaction.getWallet().getCurrencyCode());
        dto.setOriginalCurrency(transaction.getOriginalCurrency());
        dto.setExchangeRate(transaction.getExchangeRate());
        dto.setTransactionDate(transaction.getTransactionDate());
        dto.setNote(transaction.getNote());
        dto.setTransactionType(transaction.getTransactionType().getTypeName());

        WalletTransactionHistoryDTO.CategoryInfo categoryInfo = new WalletTransactionHistoryDTO.CategoryInfo();
        categoryInfo.setCategoryId(transaction.getCategory().getCategoryId());
        categoryInfo.setCategoryName(transaction.getCategory().getCategoryName());
        dto.setCategory(categoryInfo);

        dto.setCreator(buildTransactionCreator(transaction.getUser()));
        dto.setWallet(buildTransactionWalletInfo(transaction.getWallet()));

        return dto;
    }

    private WalletTransactionHistoryDTO.UserInfo buildTransactionCreator(User user) {
        WalletTransactionHistoryDTO.UserInfo info = new WalletTransactionHistoryDTO.UserInfo();
        info.setUserId(user.getUserId());
        info.setFullName(user.getFullName());
        info.setEmail(user.getEmail());
        info.setAvatar(user.getAvatar());
        return info;
    }

    private WalletTransactionHistoryDTO.WalletInfo buildTransactionWalletInfo(Wallet wallet) {
        WalletTransactionHistoryDTO.WalletInfo walletInfo = new WalletTransactionHistoryDTO.WalletInfo();
        walletInfo.setWalletId(wallet.getWalletId());
        walletInfo.setWalletName(wallet.getWalletName());
        walletInfo.setCurrencyCode(wallet.getCurrencyCode());
        walletInfo.setDeleted(wallet.isDeleted());
        return walletInfo;
    }

    private WalletTransferHistoryDTO mapTransferHistory(WalletTransfer transfer, Long walletId) {
        WalletTransferHistoryDTO dto = new WalletTransferHistoryDTO();
        dto.setTransferId(transfer.getTransferId());
        dto.setAmount(transfer.getAmount());
        dto.setOriginalAmount(transfer.getOriginalAmount());
        dto.setCurrencyCode(transfer.getCurrencyCode());
        dto.setOriginalCurrency(transfer.getOriginalCurrency());
        dto.setExchangeRate(transfer.getExchangeRate());
        dto.setTransferDate(transfer.getTransferDate());
        dto.setCreatedAt(transfer.getCreatedAt());
        dto.setUpdatedAt(transfer.getUpdatedAt());
        dto.setNote(transfer.getNote());
        dto.setStatus(transfer.getStatus().name());
        dto.setDirection(resolveDirection(transfer, walletId));
        dto.setCreator(buildTransferCreator(transfer.getUser()));
        dto.setFromWallet(buildWalletEdge(transfer.getFromWallet()));
        dto.setToWallet(buildWalletEdge(transfer.getToWallet()));
        return dto;
    }

    private WalletTransferHistoryDTO.Direction resolveDirection(WalletTransfer transfer, Long walletId) {
        boolean isSender = transfer.getFromWallet().getWalletId().equals(walletId);
        boolean isReceiver = transfer.getToWallet().getWalletId().equals(walletId);

        if (isSender && isReceiver) {
            return WalletTransferHistoryDTO.Direction.INTERNAL;
        }
        if (isSender) {
            return WalletTransferHistoryDTO.Direction.OUTGOING;
        }
        if (isReceiver) {
            return WalletTransferHistoryDTO.Direction.INCOMING;
        }
        return WalletTransferHistoryDTO.Direction.INTERNAL;
    }

    private WalletTransferHistoryDTO.UserInfo buildTransferCreator(User user) {
        WalletTransferHistoryDTO.UserInfo info = new WalletTransferHistoryDTO.UserInfo();
        info.setUserId(user.getUserId());
        info.setFullName(user.getFullName());
        info.setEmail(user.getEmail());
        info.setAvatar(user.getAvatar());
        return info;
    }

    private WalletTransferHistoryDTO.WalletEdge buildWalletEdge(Wallet wallet) {
        WalletTransferHistoryDTO.WalletEdge edge = new WalletTransferHistoryDTO.WalletEdge();
        edge.setWalletId(wallet.getWalletId());
        edge.setWalletName(wallet.getWalletName());
        edge.setCurrencyCode(wallet.getCurrencyCode());
        edge.setDeleted(wallet.isDeleted());
        return edge;
    }
}
