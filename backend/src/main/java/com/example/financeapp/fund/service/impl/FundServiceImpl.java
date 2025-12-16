package com.example.financeapp.fund.service.impl;

import com.example.financeapp.fund.dto.CreateFundRequest;
import com.example.financeapp.fund.dto.FundMemberResponse;
import com.example.financeapp.fund.dto.FundResponse;
import com.example.financeapp.fund.dto.FundTransactionResponse;
import com.example.financeapp.fund.dto.UpdateFundRequest;
import com.example.financeapp.fund.entity.Fund;
import com.example.financeapp.fund.entity.FundMember;
import com.example.financeapp.fund.entity.FundMemberRole;
import com.example.financeapp.fund.entity.FundStatus;
import com.example.financeapp.fund.entity.FundTransaction;
import com.example.financeapp.fund.entity.FundTransactionStatus;
import com.example.financeapp.fund.entity.FundTransactionType;
import com.example.financeapp.fund.entity.FundType;
import com.example.financeapp.fund.repository.FundMemberRepository;
import com.example.financeapp.fund.repository.FundRepository;
import com.example.financeapp.fund.repository.FundTransactionRepository;
import com.example.financeapp.fund.service.FundService;
import com.example.financeapp.notification.service.NotificationService;
import com.example.financeapp.email.EmailService;
import com.example.financeapp.user.entity.User;
import com.example.financeapp.user.repository.UserRepository;
import com.example.financeapp.wallet.entity.Wallet;
import com.example.financeapp.wallet.repository.WalletRepository;
import com.example.financeapp.wallet.service.WalletService;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FundServiceImpl implements FundService {

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private FundMemberRepository fundMemberRepository;

    @Autowired
    private FundTransactionRepository fundTransactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private com.example.financeapp.wallet.repository.WalletTransferRepository walletTransferRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    private void ensureNotDeleted(Fund fund) {
        if (Boolean.TRUE.equals(fund.getDeleted())) {
            throw new RuntimeException("Quỹ đã bị xóa (mềm)");
        }
    }

    private void clearPendingAutoTopup(Fund fund) {
        fund.setPendingAutoTopupAmount(BigDecimal.ZERO);
        fund.setPendingAutoTopupAt(null);
    }

    @Override
    @Transactional
    public FundResponse createFund(Long userId, CreateFundRequest request) {
        // 1. Kiểm tra user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // 2. Kiểm tra và lấy ví nguồn (source wallet)
        Wallet sourceWallet = walletRepository.findById(request.getSourceWalletId())
                .orElseThrow(() -> new RuntimeException("Ví nguồn không tồn tại"));

        if (!walletService.hasAccess(sourceWallet.getWalletId(), userId)) {
            throw new RuntimeException("Bạn không có quyền truy cập ví nguồn này");
        }

        // Kiểm tra ví nguồn có đang được sử dụng làm ví quỹ/ngân sách không
        if (isWalletUsed(sourceWallet.getWalletId())) {
            throw new RuntimeException("Ví nguồn đã được sử dụng cho quỹ hoặc ngân sách khác");
        }

        // 3. Validate theo loại quỹ và kỳ hạn
        validateFundRequest(request);

        // 4. Đánh dấu source wallet là ví quỹ
        sourceWallet.setFundWallet(true);
        walletRepository.save(sourceWallet);

        // 5. TỰ ĐỘNG TẠO VÍ QUỸ (Target Wallet)
        Wallet targetWallet = new Wallet();
        targetWallet.setUser(user);
        targetWallet.setWalletName(request.getFundName() + " - Ví Quỹ");
        targetWallet.setCurrencyCode(sourceWallet.getCurrencyCode()); // Cùng loại tiền với ví nguồn
        targetWallet.setBalance(BigDecimal.ZERO); // Bắt đầu từ 0
        targetWallet.setWalletType("PERSONAL");
        targetWallet.setFundWallet(true); // ✨ Đánh dấu đây là ví quỹ
        targetWallet.setDescription("Ví quỹ tự động tạo cho: " + request.getFundName());
        targetWallet = walletRepository.save(targetWallet);

        // 6. Tạo quỹ
        Fund fund = new Fund();
        fund.setOwner(user);
        fund.setTargetWallet(targetWallet); // Ví quỹ vừa tạo
        fund.setSourceWallet(sourceWallet); // Ví nguồn để nạp tiền
        fund.setFundType(request.getFundType());
        fund.setFundName(request.getFundName());
        fund.setHasDeadline(request.getHasDeadline());
        fund.setStatus(FundStatus.ACTIVE);
        fund.setCurrentAmount(BigDecimal.ZERO); // Bắt đầu từ 0
        fund.setNote(request.getNote());

        // Set các trường theo hasDeadline
        if (request.getHasDeadline()) {
            // Có kỳ hạn: bắt buộc các trường
            fund.setTargetAmount(request.getTargetAmount());
            fund.setFrequency(request.getFrequency());
            fund.setAmountPerPeriod(request.getAmountPerPeriod());
            fund.setStartDate(request.getStartDate());
            fund.setEndDate(request.getEndDate());
        } else {
            // Không kỳ hạn: các trường này tùy chọn
            fund.setTargetAmount(null);
            fund.setFrequency(request.getFrequency());
            fund.setAmountPerPeriod(request.getAmountPerPeriod());
            fund.setStartDate(request.getStartDate());
            fund.setEndDate(null);
        }

        // 6. Set reminder
        if (request.getReminderEnabled() != null && request.getReminderEnabled()) {
            fund.setReminderEnabled(true);
            fund.setReminderType(request.getReminderType());
            fund.setReminderTime(request.getReminderTime());
            fund.setReminderDayOfWeek(request.getReminderDayOfWeek());
            fund.setReminderDayOfMonth(request.getReminderDayOfMonth());
            fund.setReminderMonth(request.getReminderMonth());
            fund.setReminderDay(request.getReminderDay());
        } else {
            fund.setReminderEnabled(false);
        }

        // 7. Set auto deposit (đơn giản hơn - theo tần suất của quỹ)
        if (request.getAutoDepositEnabled() != null && request.getAutoDepositEnabled()) {
            fund.setAutoDepositEnabled(true);
            fund.setAutoDepositScheduleType(request.getAutoDepositScheduleType());
            fund.setAutoDepositTime(request.getAutoDepositTime());
            fund.setAutoDepositDayOfWeek(request.getAutoDepositDayOfWeek());
            fund.setAutoDepositDayOfMonth(request.getAutoDepositDayOfMonth());
            fund.setAutoDepositMonth(request.getAutoDepositMonth());
            fund.setAutoDepositDay(request.getAutoDepositDay());
            fund.setAutoDepositAmount(request.getAutoDepositAmount());
            fund.setAutoDepositStartAt(resolveAutoDepositStartAt(
                    request.getAutoDepositStartAt(),
                    request.getAutoDepositTime(),
                    request.getStartDate()
            ));
            // autoDepositType không còn cần thiết vì chỉ có 1 mode
        } else {
            fund.setAutoDepositEnabled(false);
            fund.setAutoDepositStartAt(null);
        }

        fund = fundRepository.save(fund);

        // 8. Tạo thành viên cho quỹ nhóm (nếu cần)
        if (request.getFundType() == FundType.GROUP) {
            if (request.getMembers() == null || request.getMembers().isEmpty()) {
                throw new RuntimeException("Quỹ nhóm phải có ít nhất 01 thành viên ngoài chủ quỹ");
            }

            // Tạo chủ quỹ
            FundMember ownerMember = new FundMember();
            ownerMember.setFund(fund);
            ownerMember.setUser(user);
            ownerMember.setRole(FundMemberRole.OWNER);
            fundMemberRepository.save(ownerMember);

            // Tạo các thành viên khác
            for (CreateFundRequest.FundMemberRequest memberReq : request.getMembers()) {
                User memberUser = userRepository.findByEmail(memberReq.getEmail())
                        .orElseThrow(() -> new RuntimeException(
                                "Tài khoản không tồn tại. Vui lòng mời người dùng đăng ký trước khi tham gia quỹ: " + memberReq.getEmail()));

                if (memberUser.getUserId().equals(userId)) {
                    throw new RuntimeException("Email thành viên bị trùng với chủ quỹ");
                }

                // Kiểm tra trùng email
                if (fundMemberRepository.existsByFund_FundIdAndUser_UserId(fund.getFundId(), memberUser.getUserId())) {
                    throw new RuntimeException("Email thành viên bị trùng: " + memberReq.getEmail());
                }

                FundMember member = new FundMember();
                member.setFund(fund);
                member.setUser(memberUser);
                member.setRole("CONTRIBUTOR".equals(memberReq.getRole()) ? FundMemberRole.CONTRIBUTOR : FundMemberRole.OWNER);
                fundMemberRepository.save(member);
            }
        }

        return buildFundResponse(fund);
    }

    @Override
    public List<FundResponse> getAllFunds(Long userId) {
        List<Fund> funds = fundRepository.findByUserInvolved(userId);
        return funds.stream()
                .map(this::buildFundResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<FundResponse> getPersonalFunds(Long userId, Boolean hasDeadline) {
        List<Fund> funds = fundRepository.findByOwner_UserIdAndFundTypeOrderByCreatedAtDesc(userId, FundType.PERSONAL);

        if (hasDeadline != null) {
            funds = funds.stream()
                    .filter(f -> f.getHasDeadline().equals(hasDeadline))
                    .collect(Collectors.toList());
        }

        // Ẩn quỹ đã tất toán (CLOSED) và đã hoàn thành (COMPLETED) khỏi danh sách quỹ
        // (Nhưng vẫn hiển thị trong báo cáo qua getAllFunds)
        funds = funds.stream()
                .filter(f -> {
                    // Chỉ hiển thị quỹ có status ACTIVE (ẩn COMPLETED và CLOSED)
                    return f.getStatus() == FundStatus.ACTIVE;
                })
                .collect(Collectors.toList());

        return funds.stream()
                .map(this::buildFundResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<FundResponse> getGroupFunds(Long userId, Boolean hasDeadline) {
        List<Fund> funds = fundRepository.findByOwner_UserIdAndFundTypeAndStatusOrderByCreatedAtDesc(
                userId, FundType.GROUP, FundStatus.ACTIVE);

        if (hasDeadline != null) {
            funds = funds.stream()
                    .filter(f -> f.getHasDeadline().equals(hasDeadline))
                    .collect(Collectors.toList());
        }

        return funds.stream()
                .map(this::buildFundResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<FundResponse> getParticipatedFunds(Long userId) {
        List<Fund> funds = fundMemberRepository.findGroupFundsByMember(userId);
        return funds.stream()
                .map(this::buildFundResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FundResponse getFundById(Long userId, Long fundId) {
        Fund fund = fundRepository.findByIdWithRelations(fundId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy quỹ"));
        ensureNotDeleted(fund);

        // Kiểm tra quyền: user phải là chủ quỹ hoặc thành viên
        if (!fund.getOwner().getUserId().equals(userId) &&
                !fundMemberRepository.existsByFund_FundIdAndUser_UserId(fundId, userId)) {
            throw new RuntimeException("Bạn không có quyền xem quỹ này");
        }

        return buildFundResponse(fund);
    }

    @Override
    @Transactional
    public FundResponse updateFund(Long userId, Long fundId, UpdateFundRequest request) {
        Fund fund = fundRepository.findByIdWithRelations(fundId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy quỹ"));
        ensureNotDeleted(fund);

        // Kiểm tra quyền: chỉ chủ quỹ mới được sửa
        if (!fund.getOwner().getUserId().equals(userId)) {
            throw new RuntimeException("Chỉ chủ quỹ mới được sửa thông tin quỹ");
        }

        if (fund.getStatus() != FundStatus.ACTIVE) {
            throw new RuntimeException("Không thể sửa quỹ đã đóng hoặc đã hoàn thành");
        }

        // Cập nhật các trường được phép sửa
        if (request.getFundName() != null) {
            fund.setFundName(request.getFundName());
        }
        if (request.getFrequency() != null) {
            fund.setFrequency(request.getFrequency());
        }
        if (request.getAmountPerPeriod() != null) {
            fund.setAmountPerPeriod(request.getAmountPerPeriod());
        }
        if (request.getStartDate() != null) {
            fund.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null && fund.getHasDeadline()) {
            fund.setEndDate(request.getEndDate());
        }
        if (request.getNote() != null) {
            fund.setNote(request.getNote());
        }

        // Cập nhật reminder
        if (request.getReminderEnabled() != null) {
            fund.setReminderEnabled(request.getReminderEnabled());
            if (request.getReminderEnabled()) {
                fund.setReminderType(request.getReminderType());
                fund.setReminderTime(request.getReminderTime());
                fund.setReminderDayOfWeek(request.getReminderDayOfWeek());
                fund.setReminderDayOfMonth(request.getReminderDayOfMonth());
                fund.setReminderMonth(request.getReminderMonth());
                fund.setReminderDay(request.getReminderDay());
            }
        }

        // Cập nhật auto deposit
        if (request.getAutoDepositEnabled() != null) {
            fund.setAutoDepositEnabled(request.getAutoDepositEnabled());
            if (request.getAutoDepositEnabled()) {
                // Kiểm tra xem thời gian auto-deposit có thay đổi không
                boolean timeChanged = false;
                if (request.getAutoDepositTime() != null && fund.getAutoDepositTime() != null) {
                    if (!request.getAutoDepositTime().equals(fund.getAutoDepositTime())) {
                        timeChanged = true;
                    }
                } else if (request.getAutoDepositTime() != null || fund.getAutoDepositTime() != null) {
                    timeChanged = true;
                }

                // Kiểm tra xem schedule type có thay đổi không
                boolean scheduleChanged = false;
                if (request.getAutoDepositScheduleType() != null && fund.getAutoDepositScheduleType() != null) {
                    if (!request.getAutoDepositScheduleType().equals(fund.getAutoDepositScheduleType())) {
                        scheduleChanged = true;
                    }
                } else if (request.getAutoDepositScheduleType() != null || fund.getAutoDepositScheduleType() != null) {
                    scheduleChanged = true;
                }

                // Kiểm tra dayOfWeek/dayOfMonth có thay đổi không
                boolean dayChanged = false;
                if (request.getAutoDepositScheduleType() != null) {
                    if (request.getAutoDepositScheduleType() == com.example.financeapp.fund.entity.ReminderType.WEEKLY) {
                        if (request.getAutoDepositDayOfWeek() != null && fund.getAutoDepositDayOfWeek() != null) {
                            if (!request.getAutoDepositDayOfWeek().equals(fund.getAutoDepositDayOfWeek())) {
                                dayChanged = true;
                            }
                        } else if (request.getAutoDepositDayOfWeek() != null || fund.getAutoDepositDayOfWeek() != null) {
                            dayChanged = true;
                        }
                    } else if (request.getAutoDepositScheduleType() == com.example.financeapp.fund.entity.ReminderType.MONTHLY) {
                        if (request.getAutoDepositDayOfMonth() != null && fund.getAutoDepositDayOfMonth() != null) {
                            if (!request.getAutoDepositDayOfMonth().equals(fund.getAutoDepositDayOfMonth())) {
                                dayChanged = true;
                            }
                        } else if (request.getAutoDepositDayOfMonth() != null || fund.getAutoDepositDayOfMonth() != null) {
                            dayChanged = true;
                        }
                    }
                }

                // Nếu thời gian hoặc lịch trình thay đổi, cần đảm bảo lần nạp tiếp theo sử dụng thời gian mới
                // Bằng cách kiểm tra xem đã nạp trong chu kỳ hiện tại chưa, nếu chưa thì có thể nạp với thời gian mới
                // Cập nhật thông tin tự động nạp tiền
                fund.setAutoDepositScheduleType(request.getAutoDepositScheduleType());
                fund.setAutoDepositTime(request.getAutoDepositTime());
                fund.setAutoDepositDayOfWeek(request.getAutoDepositDayOfWeek());
                fund.setAutoDepositDayOfMonth(request.getAutoDepositDayOfMonth());
                fund.setAutoDepositMonth(request.getAutoDepositMonth());
                fund.setAutoDepositDay(request.getAutoDepositDay());
                fund.setAutoDepositAmount(request.getAutoDepositAmount());

                // Nếu thời gian hoặc lịch trình thay đổi, reset autoDepositStartAt để áp dụng lịch mới
                if (timeChanged || scheduleChanged || dayChanged) {
                    // Reset để lần nạp tiếp theo sử dụng thời gian mới
                    // Nhưng vẫn kiểm tra xem đã nạp trong chu kỳ hiện tại chưa (logic trong query)
                    fund.setAutoDepositStartAt(null);
                } else if (request.getAutoDepositStartAt() != null) {
                    fund.setAutoDepositStartAt(request.getAutoDepositStartAt());
                } else if (fund.getAutoDepositStartAt() == null) {
                    fund.setAutoDepositStartAt(resolveAutoDepositStartAt(
                            null,
                            request.getAutoDepositTime(),
                            fund.getStartDate()
                    ));
                }
                // Note: sourceWallet không thể thay đổi sau khi tạo quỹ
            }
        }

        // Cập nhật thành viên (cho quỹ nhóm)
        if (request.getMembers() != null && fund.getFundType() == FundType.GROUP) {
            // Logic cập nhật thành viên sẽ được xử lý riêng
            // Ở đây chỉ validate
        }

        fund = fundRepository.save(fund);
        return buildFundResponse(fund);
    }

    @Override
    @Transactional
    public void closeFund(Long userId, Long fundId) {
        Fund fund = fundRepository.findByIdWithRelations(fundId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy quỹ"));
        ensureNotDeleted(fund);

        if (!fund.getOwner().getUserId().equals(userId)) {
            throw new RuntimeException("Chỉ chủ quỹ mới được đóng quỹ");
        }

        fund.setStatus(FundStatus.CLOSED);
        fundRepository.save(fund);
    }

    @Override
    @Transactional
    public void deleteFund(Long userId, Long fundId) {
        Fund fund = fundRepository.findByIdWithRelations(fundId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy quỹ"));
        if (Boolean.TRUE.equals(fund.getDeleted())) {
            return; // đã xóa mềm
        }

        if (!fund.getOwner().getUserId().equals(userId)) {
            throw new RuntimeException("Chỉ chủ quỹ mới được xóa quỹ");
        }

        // Lưu thông tin wallets trước khi xóa
        Wallet sourceWallet = fund.getSourceWallet();
        Wallet targetWallet = fund.getTargetWallet();

        // Xóa mềm fund
        fund.setStatus(FundStatus.CLOSED);
        fund.setDeleted(true);
        fund.setDeletedAt(LocalDateTime.now());
        fundRepository.save(fund);

        // Kiểm tra và cập nhật isFundWallet cho source wallet
        if (sourceWallet != null) {
            // Kiểm tra xem source wallet có còn được dùng bởi fund khác không (chưa bị xóa)
            long activeFundsCount = fundRepository.countBySourceWallet_WalletIdAndDeletedFalse(
                    sourceWallet.getWalletId());
            if (activeFundsCount == 0) {
                // Không còn fund nào dùng source wallet này, bỏ đánh dấu ví quỹ
                sourceWallet.setFundWallet(false);
                walletRepository.save(sourceWallet);
            }
        }

        // Bỏ đánh dấu ví quỹ cho target wallet (vì ví quỹ chỉ dùng cho fund này)
        if (targetWallet != null) {
            targetWallet.setFundWallet(false);
            walletRepository.save(targetWallet);
        }
    }

    @Override
    @Transactional
    public FundResponse depositToFund(Long userId, Long fundId, BigDecimal amount, FundTransactionType type, String message) {
        Fund fund = fundRepository.findByIdWithRelations(fundId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy quỹ"));
        ensureNotDeleted(fund);
        FundTransactionType effectiveType = (type != null) ? type : FundTransactionType.DEPOSIT;

        // Kiểm tra quyền
        if (!fund.getOwner().getUserId().equals(userId) &&
                !fundMemberRepository.existsByFund_FundIdAndUser_UserId(fundId, userId)) {
            throw new RuntimeException("Bạn không có quyền nạp tiền vào quỹ này");
        }

        if (fund.getStatus() != FundStatus.ACTIVE) {
            throw new RuntimeException("Không thể nạp tiền vào quỹ đã đóng");
        }

        if (effectiveType == FundTransactionType.AUTO_DEPOSIT_RECOVERY) {
            BigDecimal pending = fund.getPendingAutoTopupAmount() != null ? fund.getPendingAutoTopupAmount() : BigDecimal.ZERO;
            if (pending.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Không có khoản nạp bù đang chờ");
            }
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Số tiền nạp phải lớn hơn 0");
        }

        // Validation logic: Kiểm tra số tiền nạp theo tần suất (chỉ cho manual deposit)
        // Bỏ qua validation cho AUTO_DEPOSIT và AUTO_DEPOSIT_RECOVERY
        if (effectiveType == FundTransactionType.DEPOSIT && fund.getAmountPerPeriod() != null) {
            BigDecimal amountPerPeriod = fund.getAmountPerPeriod();
            BigDecimal minAmount = new BigDecimal("1000"); // Tối thiểu 1,000 VND

            // Tính tổng số tiền đã nạp hôm nay (manual deposit)
            List<FundTransaction> todayDeposits = fundTransactionRepository.findTodayManualDeposits(fundId);
            BigDecimal todayDepositedAmount = todayDeposits.stream()
                    .map(FundTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Kiểm tra xem hôm nay đã nạp đủ tần suất chưa
            boolean hasEnoughToday = todayDepositedAmount.compareTo(amountPerPeriod) >= 0;

            if (!hasEnoughToday) {
                // Chưa nạp đủ hôm nay: yêu cầu amount >= amountPerPeriod
                if (amount.compareTo(amountPerPeriod) < 0) {
                    throw new RuntimeException(
                            String.format("Số tiền nạp phải lớn hơn hoặc bằng số tiền theo tần suất: %,.0f %s",
                                    amountPerPeriod, fund.getTargetWallet().getCurrencyCode()));
                }
            } else {
                // Đã nạp đủ hôm nay: chỉ yêu cầu amount >= 1,000 VND
                if (amount.compareTo(minAmount) < 0) {
                    throw new RuntimeException(
                            String.format("Số tiền nạp tối thiểu là %,.0f %s",
                                    minAmount, fund.getTargetWallet().getCurrencyCode()));
                }
            }
        } else if (effectiveType == FundTransactionType.DEPOSIT) {
            // Không có amountPerPeriod: chỉ yêu cầu >= 1,000 VND
            BigDecimal minAmount = new BigDecimal("1000");
            if (amount.compareTo(minAmount) < 0) {
                throw new RuntimeException(
                        String.format("Số tiền nạp tối thiểu là %,.0f %s",
                                minAmount, fund.getTargetWallet().getCurrencyCode()));
            }
        }

        // Lấy ví nguồn và ví đích với lock để đảm bảo nhất quán số dư
        Wallet sourceWallet = walletRepository.findByIdWithLock(fund.getSourceWallet().getWalletId())
                .orElseThrow(() -> new RuntimeException("Ví nguồn không tồn tại"));
        Wallet targetWallet = walletRepository.findByIdWithLock(fund.getTargetWallet().getWalletId())
                .orElseThrow(() -> new RuntimeException("Ví đích không tồn tại"));

        // Kiểm tra user có quyền trên ví nguồn
        if (!walletService.hasAccess(sourceWallet.getWalletId(), userId)) {
            throw new RuntimeException("Bạn không có quyền truy cập ví nguồn của quỹ");
        }

        // Kiểm tra đủ số dư ví nguồn
        if (sourceWallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Số dư ví nguồn không đủ để nạp số tiền này vào quỹ");
        }

        // Lưu số dư trước khi thay đổi để ghi lịch sử transfer
        java.math.BigDecimal sourceBefore = sourceWallet.getBalance();
        java.math.BigDecimal targetBefore = targetWallet.getBalance();

        // Trừ ví nguồn, cộng ví đích
        sourceWallet.setBalance(sourceWallet.getBalance().subtract(amount));
        targetWallet.setBalance(targetWallet.getBalance().add(amount));
        walletRepository.save(sourceWallet);
        walletRepository.save(targetWallet);

        // Cập nhật số tiền quỹ
        fund.setCurrentAmount(fund.getCurrentAmount().add(amount));
        if (effectiveType == FundTransactionType.AUTO_DEPOSIT_RECOVERY) {
            BigDecimal pending = fund.getPendingAutoTopupAmount() != null ? fund.getPendingAutoTopupAmount() : BigDecimal.ZERO;
            BigDecimal newPending = pending.subtract(amount);
            if (newPending.compareTo(BigDecimal.ZERO) < 0) newPending = BigDecimal.ZERO;
            fund.setPendingAutoTopupAmount(newPending);
            if (newPending.compareTo(BigDecimal.ZERO) == 0) {
                fund.setPendingAutoTopupAt(null);
            }
        }

        // Nếu là quỹ có kỳ hạn và đã đạt mục tiêu, chỉ đánh dấu trạng thái COMPLETED
        // nhưng KHÔNG tự động rút tiền về ví nguồn. Việc rút sẽ do người dùng thực hiện
        // thủ công qua flow "Rút toàn bộ về ví nguồn" trên UI.
        if (fund.getHasDeadline() && fund.getTargetAmount() != null &&
                fund.getCurrentAmount().compareTo(fund.getTargetAmount()) >= 0) {
            fund.setStatus(FundStatus.COMPLETED);
        }

        fund = fundRepository.save(fund);

        // Tạo bản ghi WalletTransfer để hiển thị trong lịch sử chuyển khoản
        try {
            com.example.financeapp.wallet.entity.WalletTransfer transfer = new com.example.financeapp.wallet.entity.WalletTransfer();
            transfer.setFromWallet(sourceWallet);
            transfer.setToWallet(targetWallet);
            transfer.setAmount(amount);
            transfer.setCurrencyCode(sourceWallet.getCurrencyCode());
            transfer.setUser(fund.getOwner());
            transfer.setNote("Nạp vào quỹ: " + fund.getFundName());
            transfer.setTransferDate(java.time.LocalDateTime.now());
            transfer.setStatus(com.example.financeapp.wallet.entity.WalletTransfer.TransferStatus.COMPLETED);
            transfer.setFromBalanceBefore(sourceBefore);
            transfer.setFromBalanceAfter(sourceWallet.getBalance());
            transfer.setToBalanceBefore(targetBefore);
            transfer.setToBalanceAfter(targetWallet.getBalance());

            walletTransferRepository.save(transfer);
        } catch (Exception ex) {
            // Không block flow nếu ghi lịch sử thất bại; chỉ log
            System.err.println("Không thể ghi WalletTransfer sau khi nạp quỹ: " + ex.getMessage());
        }

        // Lưu lịch sử giao dịch quỹ
        User performer = userRepository.findById(userId)
                .orElse(fund.getOwner());
        FundTransaction tx = new FundTransaction();
        tx.setFund(fund);
        tx.setAmount(amount);
        tx.setType(effectiveType);
        tx.setStatus(FundTransactionStatus.SUCCESS);
        tx.setMessage(message);
        tx.setPerformedBy(performer);
        fundTransactionRepository.save(tx);

        if (effectiveType == FundTransactionType.AUTO_DEPOSIT_RECOVERY) {
            try {
                String email = performer.getEmail();
                String fullName = performer.getFullName() != null ? performer.getFullName() : performer.getEmail();
                if (email != null && !email.isBlank()) {
                    String subject = "[MyWallet] ✅ Nạp bù quỹ thành công";
                    String content = "Xin chào " + fullName + ",\n\n"
                            + "Hệ thống đã nạp bù quỹ của bạn sau khi lần nạp tự động trước đó thất bại.\n\n"
                            + "📊 Chi tiết:\n"
                            + "   • Quỹ: " + fund.getFundName() + "\n"
                            + "   • Số tiền nạp bù: " + String.format("%,.0f", amount) + " " + fund.getTargetWallet().getCurrencyCode() + "\n"
                            + "   • Từ ví: " + (fund.getSourceWallet() != null ? fund.getSourceWallet().getWalletName() : "Ví nguồn") + "\n"
                            + "   • Số dư mới trong quỹ: " + String.format("%,.0f", fund.getCurrentAmount()) + " " + fund.getTargetWallet().getCurrencyCode() + "\n"
                            + "\n"
                            + "Cảm ơn bạn đã tiếp tục đồng hành cùng MyWallet.\n\n"
                            + "Trân trọng,\nĐội ngũ MyWallet";
                    emailService.sendEmail(email, subject, content);
                }
            } catch (Exception ignore) {
                // Không chặn flow nếu gửi email lỗi
            }
        }

        return buildFundResponse(fund);
    }

    @Override
    @Transactional
    public FundResponse withdrawFromFund(Long userId, Long fundId, BigDecimal amount) {
        Fund fund = fundRepository.findByIdWithRelations(fundId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy quỹ"));
        ensureNotDeleted(fund);

        // Nếu quỹ có kỳ hạn: chỉ cho rút khi đã hoàn thành (COMPLETED)
        if (fund.getHasDeadline()) {
            if (fund.getStatus() != FundStatus.COMPLETED) {
                throw new RuntimeException("Quỹ có kỳ hạn chưa hoàn thành, không thể rút tiền");
            }
        }

        // Kiểm tra quyền
        if (!fund.getOwner().getUserId().equals(userId)) {
            throw new RuntimeException("Chỉ chủ quỹ mới được rút tiền");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Số tiền rút phải lớn hơn 0");
        }

        if (fund.getCurrentAmount().compareTo(amount) < 0) {
            throw new RuntimeException("Số tiền trong quỹ không đủ để rút");
        }

        // Kiểm tra số tiền hợp lệ
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Số tiền rút phải lớn hơn 0");
        }

        if (fund.getCurrentAmount().compareTo(amount) < 0) {
            throw new RuntimeException("Số tiền trong quỹ không đủ để rút");
        }

        // Lấy ví đích với lock
        Wallet targetWallet = walletRepository.findByIdWithLock(fund.getTargetWallet().getWalletId())
                .orElseThrow(() -> new RuntimeException("Ví đích không tồn tại"));

        if (targetWallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Số dư ví quỹ không đủ để rút số tiền này");
        }

        // Lấy ví nguồn với lock (cho cả quỹ có kỳ hạn và không kỳ hạn)
        Wallet sourceWallet = walletRepository.findByIdWithLock(fund.getSourceWallet().getWalletId())
                .orElseThrow(() -> new RuntimeException("Ví nguồn không tồn tại"));

        // Kiểm tra quyền trên ví nguồn (dù chủ quỹ thường là chủ ví nguồn)
        if (!walletService.hasAccess(sourceWallet.getWalletId(), userId)) {
            throw new RuntimeException("Bạn không có quyền truy cập ví nguồn của quỹ");
        }

        // Chuyển tiền: trừ ví quỹ, cộng ví nguồn (cho cả quỹ có kỳ hạn và không kỳ hạn)
        targetWallet.setBalance(targetWallet.getBalance().subtract(amount));
        sourceWallet.setBalance(sourceWallet.getBalance().add(amount));

        walletRepository.save(targetWallet);
        walletRepository.save(sourceWallet);

        // Trừ số tiền quỹ
        fund.setCurrentAmount(fund.getCurrentAmount().subtract(amount));

        // Nếu quỹ còn 0: chỉ đóng quỹ có thời hạn, quỹ không thời hạn vẫn giữ status ACTIVE để người dùng có thể nạp tiền lại hoặc xóa thủ công
        if (fund.getCurrentAmount().compareTo(BigDecimal.ZERO) == 0) {
            // Chỉ tự động đóng quỹ có thời hạn khi rút hết
            if (Boolean.TRUE.equals(fund.getHasDeadline())) {
                fund.setStatus(FundStatus.CLOSED);
            }
            // Quỹ không thời hạn: giữ nguyên status ACTIVE, người dùng có thể xóa thủ công hoặc nạp tiền lại
        }

        fund = fundRepository.save(fund);

        User performer = userRepository.findById(userId)
                .orElse(fund.getOwner());
        FundTransaction tx = new FundTransaction();
        tx.setFund(fund);
        tx.setAmount(amount);
        tx.setType(FundTransactionType.WITHDRAW);
        tx.setStatus(FundTransactionStatus.SUCCESS);
        tx.setMessage("Rút tiền khỏi quỹ");
        tx.setPerformedBy(performer);
        fundTransactionRepository.save(tx);

        return buildFundResponse(fund);
    }

    @Override
    @Transactional
    public FundResponse settleFund(Long userId, Long fundId) {
        Fund fund = fundRepository.findByIdWithRelations(fundId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy quỹ"));
        ensureNotDeleted(fund);

        // Kiểm tra quyền
        if (!fund.getOwner().getUserId().equals(userId)) {
            throw new RuntimeException("Chỉ chủ quỹ mới được tất toán quỹ");
        }

        BigDecimal currentAmount = fund.getCurrentAmount();
        if (currentAmount == null || currentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            // Không có tiền để tất toán, chỉ đóng quỹ
            fund.setStatus(FundStatus.CLOSED);
            fund = fundRepository.save(fund);
            return buildFundResponse(fund);
        }

        // Lấy ví quỹ và ví nguồn với lock
        Wallet targetWallet = walletRepository.findByIdWithLock(fund.getTargetWallet().getWalletId())
                .orElseThrow(() -> new RuntimeException("Ví quỹ không tồn tại"));

        Wallet sourceWallet = walletRepository.findByIdWithLock(fund.getSourceWallet().getWalletId())
                .orElseThrow(() -> new RuntimeException("Ví nguồn không tồn tại"));

        // Kiểm tra quyền trên ví nguồn
        if (!walletService.hasAccess(sourceWallet.getWalletId(), userId)) {
            throw new RuntimeException("Bạn không có quyền truy cập ví nguồn của quỹ");
        }

        // Điều chỉnh số tiền nếu số dư ví quỹ không đủ
        BigDecimal actualAmount = currentAmount;
        if (targetWallet.getBalance().compareTo(currentAmount) < 0) {
            actualAmount = targetWallet.getBalance();
        }

        // Chuyển toàn bộ tiền từ ví quỹ về ví nguồn
        targetWallet.setBalance(targetWallet.getBalance().subtract(actualAmount));
        sourceWallet.setBalance(sourceWallet.getBalance().add(actualAmount));

        walletRepository.save(targetWallet);
        walletRepository.save(sourceWallet);

        // Cập nhật quỹ: số dư = 0, trạng thái = CLOSED
        fund.setCurrentAmount(BigDecimal.ZERO);
        fund.setStatus(FundStatus.CLOSED);
        fund = fundRepository.save(fund);

        // Ghi vào lịch sử
        User performer = userRepository.findById(userId)
                .orElse(fund.getOwner());
        FundTransaction tx = new FundTransaction();
        tx.setFund(fund);
        tx.setAmount(actualAmount);
        tx.setType(FundTransactionType.WITHDRAW);
        tx.setStatus(FundTransactionStatus.SUCCESS);
        tx.setMessage("Tất toán quỹ");
        tx.setPerformedBy(performer);
        fundTransactionRepository.save(tx);

        return buildFundResponse(fund);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FundTransactionResponse> getFundTransactions(Long userId, Long fundId, int limit) {
        Fund fund = fundRepository.findByIdWithRelations(fundId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy quỹ"));
        // cho phép xem cả khi quỹ đã xóa mềm để giữ báo cáo/lich sử
        if (!fund.getOwner().getUserId().equals(userId) &&
                !fundMemberRepository.existsByFund_FundIdAndUser_UserId(fundId, userId)) {
            throw new RuntimeException("Bạn không có quyền xem lịch sử quỹ này");
        }

        int pageSize = limit <= 0 ? 50 : Math.min(limit, 200);
        return fundTransactionRepository.findByFundId(fundId, PageRequest.of(0, pageSize))
                .stream()
                .map(FundTransactionResponse::from)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Tất toán quỹ có kỳ hạn khi đạt mục tiêu và chuyển toàn bộ tiền từ ví quỹ
     * về ví nguồn (sourceWallet).
     * Được gọi bên trong các transaction khác, nên không cần @Transactional riêng.
     */
    protected void settleFundAndTransferToSourceWallet(Fund fund) {
        if (fund == null) {
            throw new RuntimeException("Quỹ không hợp lệ");
        }

        if (!Boolean.TRUE.equals(fund.getHasDeadline())) {
            // Chỉ áp dụng cho quỹ có kỳ hạn
            return;
        }

        BigDecimal currentAmount = fund.getCurrentAmount();
        if (currentAmount == null || currentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            // Không có tiền để chuyển
            fund.setStatus(FundStatus.COMPLETED);
            return;
        }

        Wallet sourceWallet = fund.getSourceWallet();
        Wallet targetWallet = walletRepository.findByIdWithLock(fund.getTargetWallet().getWalletId())
                .orElseThrow(() -> new RuntimeException("Ví quỹ không tồn tại"));

        if (sourceWallet == null) {
            throw new RuntimeException("Không tìm thấy ví nguồn để tất toán quỹ");
        }

        // Cập nhật số dư ví: rút toàn bộ khỏi ví quỹ, cộng vào ví nguồn
        if (targetWallet.getBalance().compareTo(currentAmount) < 0) {
            // Trường hợp dữ liệu lệch, chỉ chuyển tối đa bằng số dư ví quỹ
            currentAmount = targetWallet.getBalance();
        }

        targetWallet.setBalance(targetWallet.getBalance().subtract(currentAmount));
        sourceWallet.setBalance(sourceWallet.getBalance().add(currentAmount));

        walletRepository.save(targetWallet);
        walletRepository.save(sourceWallet);

        // Cập nhật trạng thái quỹ
        fund.setCurrentAmount(BigDecimal.ZERO);
        fund.setStatus(FundStatus.COMPLETED);
        fundRepository.save(fund);
    }

    @Override
    public boolean isWalletUsed(Long walletId) {
        // Kiểm tra ví có được dùng cho quỹ không
        if (fundRepository.existsByTargetWallet_WalletId(walletId)) {
            return true;
        }

        // Kiểm tra ví có được dùng cho ngân sách không
        // (Cần thêm method vào BudgetRepository nếu chưa có)
        return false; // Tạm thời return false, sẽ bổ sung sau
    }

    // ============ HELPER METHODS ============

    private void validateFundRequest(CreateFundRequest request) {
        LocalDate today = LocalDate.now();

        // Validate quỹ có kỳ hạn
        if (request.getHasDeadline()) {
            if (request.getTargetAmount() == null || request.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Số tiền mục tiêu phải lớn hơn 0");
            }

            // Kiểm tra số tiền mục tiêu tối thiểu
            if (request.getTargetAmount().compareTo(new BigDecimal("1000")) < 0) {
                throw new RuntimeException("Số tiền mục tiêu phải lớn hơn hoặc bằng 1,000");
            }

            if (request.getFrequency() == null) {
                throw new RuntimeException("Vui lòng chọn tần suất gửi quỹ");
            }

            if (request.getStartDate() == null) {
                throw new RuntimeException("Vui lòng chọn ngày bắt đầu");
            }

            if (request.getStartDate().isBefore(today)) {
                throw new RuntimeException("Ngày bắt đầu phải từ hôm nay trở đi");
            }

            if (request.getEndDate() == null) {
                throw new RuntimeException("Vui lòng chọn ngày kết thúc");
            }

            if (request.getEndDate().isBefore(request.getStartDate())) {
                throw new RuntimeException("Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu");
            }

            // Validate khoảng thời gian theo tần suất
            long daysBetween = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
            switch (request.getFrequency()) {
                case DAILY:
                    if (daysBetween < 1) {
                        throw new RuntimeException("Khoảng thời gian không đủ cho ít nhất một kỳ gửi");
                    }
                    break;
                case WEEKLY:
                    if (daysBetween < 7) {
                        throw new RuntimeException("Khoảng thời gian không đủ cho ít nhất một kỳ gửi");
                    }
                    break;
                case MONTHLY:
                    if (daysBetween < 30) {
                        throw new RuntimeException("Khoảng thời gian không đủ cho ít nhất một kỳ gửi");
                    }
                    break;
                case YEARLY:
                    if (daysBetween < 365) {
                        throw new RuntimeException("Khoảng thời gian không đủ cho ít nhất một kỳ gửi");
                    }
                    break;
            }
        } else {
            // Quỹ không kỳ hạn: startDate có thể null, endDate phải là null
            // Cho phép startDate = today hoặc > today (không chỉ > today)
            if (request.getStartDate() != null && request.getStartDate().isBefore(today)) {
                throw new RuntimeException("Ngày bắt đầu phải lớn hơn hoặc bằng ngày hiện tại");
            }

            // Không cần validate endDate cho quỹ không kỳ hạn
            // endDate sẽ được set thành null trong code tạo quỹ (dòng 140)
        }

        // Validate reminder
        if (request.getReminderEnabled() != null && request.getReminderEnabled()) {
            if (request.getReminderType() == null) {
                throw new RuntimeException("Vui lòng chọn kiểu nhắc nhở");
            }
            if (request.getReminderTime() == null) {
                throw new RuntimeException("Vui lòng chọn giờ nhắc");
            }
            // Validate các trường theo reminderType
            validateReminderFields(request.getReminderType(), request);
        }

        // Validate auto deposit
        if (request.getAutoDepositEnabled() != null && request.getAutoDepositEnabled()) {
            if (request.getAutoDepositScheduleType() == null) {
                throw new RuntimeException("Vui lòng chọn tần suất tự động nạp tiền");
            }

            if (request.getAutoDepositAmount() == null || request.getAutoDepositAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Số tiền mỗi lần nạp phải lớn hơn 0");
            }
        }
    }

    private LocalDateTime resolveAutoDepositStartAt(LocalDateTime requestedStartAt, LocalTime autoTime, LocalDate fallbackDate) {
        if (requestedStartAt != null) {
            return requestedStartAt;
        }

        LocalDate startDate = fallbackDate != null ? fallbackDate : LocalDate.now();
        LocalTime startTime = autoTime != null ? autoTime : LocalTime.now();
        return LocalDateTime.of(startDate, startTime);
    }

    private void validateReminderFields(com.example.financeapp.fund.entity.ReminderType reminderType, CreateFundRequest request) {
        switch (reminderType) {
            case DAILY:
                // DAILY chỉ cần time, không cần thêm field nào
                break;
            case WEEKLY:
                if (request.getReminderDayOfWeek() == null) {
                    throw new RuntimeException("Vui lòng chọn thứ trong tuần cho nhắc nhở");
                }
                break;
            case MONTHLY:
                if (request.getReminderDayOfMonth() == null) {
                    throw new RuntimeException("Vui lòng chọn ngày trong tháng cho nhắc nhở");
                }
                break;
            case YEARLY:
                if (request.getReminderMonth() == null || request.getReminderDay() == null) {
                    throw new RuntimeException("Vui lòng chọn tháng và ngày cho nhắc nhở");
                }
                break;
        }
    }

    @Override
    @Transactional
    public void tryAutoRecoverForWallet(Long walletId) {
        List<Fund> pendingFunds = fundRepository.findPendingAutoTopupBySourceWallet(walletId);
        if (pendingFunds.isEmpty()) return;

        for (Fund fund : pendingFunds) {
            try {
                BigDecimal pending = fund.getPendingAutoTopupAmount() != null ? fund.getPendingAutoTopupAmount() : BigDecimal.ZERO;
                if (pending.compareTo(BigDecimal.ZERO) <= 0) continue;

                Wallet sourceWallet = walletRepository.findByIdWithLock(fund.getSourceWallet().getWalletId())
                        .orElse(null);
                if (sourceWallet == null) continue;
                if (sourceWallet.getBalance().compareTo(pending) < 0) continue;

                depositToFund(
                        fund.getOwner().getUserId(),
                        fund.getFundId(),
                        pending,
                        FundTransactionType.AUTO_DEPOSIT_RECOVERY,
                        "Tự động nạp bù sau khi ví được nạp thêm"
                );

                try {
                    String title = "Nạp bù tự động thành công: " + fund.getFundName();
                    String msg = "Đã nạp bù " + pending + " " + fund.getTargetWallet().getCurrencyCode() + " vào quỹ.";
                    notificationService.createUserNotification(
                            fund.getOwner().getUserId(),
                            com.example.financeapp.notification.entity.Notification.NotificationType.SYSTEM_ANNOUNCEMENT,
                            title,
                            msg,
                            fund.getFundId(),
                            "FUND_AUTO_DEPOSIT_RECOVERY_SUCCESS"
                    );
                } catch (Exception ignore) {
                    // Không chặn flow nếu gửi notif thất bại
                }
            } catch (Exception e) {
                System.err.println("Không thể auto recover quỹ " + fund.getFundId() + ": " + e.getMessage());
            }
        }
    }

    private FundResponse buildFundResponse(Fund fund) {
        FundResponse response = FundResponse.fromEntity(fund);

        // Load thành viên nếu là quỹ nhóm
        if (fund.getFundType() == FundType.GROUP) {
            List<FundMember> members = fundMemberRepository.findByFund_FundIdOrderByJoinedAtAsc(fund.getFundId());
            List<FundMemberResponse> memberResponses = members.stream()
                    .map(FundMemberResponse::fromEntity)
                    .collect(Collectors.toList());
            response.setMembers(memberResponses);
            response.setMemberCount(members.size());
        }

        return response;
    }
}

