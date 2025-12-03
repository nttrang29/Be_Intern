package com.example.financeapp.fund.service.impl;

import com.example.financeapp.fund.dto.CreateFundRequest;
import com.example.financeapp.fund.dto.FundMemberResponse;
import com.example.financeapp.fund.dto.FundResponse;
import com.example.financeapp.fund.dto.UpdateFundRequest;
import com.example.financeapp.fund.entity.Fund;
import com.example.financeapp.fund.entity.FundMember;
import com.example.financeapp.fund.entity.FundMemberRole;
import com.example.financeapp.fund.entity.FundStatus;
import com.example.financeapp.fund.entity.FundType;
import com.example.financeapp.fund.repository.FundMemberRepository;
import com.example.financeapp.fund.repository.FundRepository;
import com.example.financeapp.fund.service.FundService;
import com.example.financeapp.user.entity.User;
import com.example.financeapp.user.repository.UserRepository;
import com.example.financeapp.wallet.entity.Wallet;
import com.example.financeapp.wallet.repository.WalletRepository;
import com.example.financeapp.wallet.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletService walletService;

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

        // 4. TỰ ĐỘNG TẠO VÍ QUỸ (Target Wallet)
        Wallet targetWallet = new Wallet();
        targetWallet.setUser(user);
        targetWallet.setWalletName(request.getFundName() + " - Ví Quỹ");
        targetWallet.setCurrencyCode(sourceWallet.getCurrencyCode()); // Cùng loại tiền với ví nguồn
        targetWallet.setBalance(BigDecimal.ZERO); // Bắt đầu từ 0
        targetWallet.setWalletType("PERSONAL");
        targetWallet.setDescription("Ví quỹ tự động tạo cho: " + request.getFundName());
        targetWallet = walletRepository.save(targetWallet);

        // 5. Tạo quỹ
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
            // autoDepositType không còn cần thiết vì chỉ có 1 mode
        } else {
            fund.setAutoDepositEnabled(false);
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
                // Cập nhật thông tin tự động nạp tiền
                fund.setAutoDepositScheduleType(request.getAutoDepositScheduleType());
                fund.setAutoDepositTime(request.getAutoDepositTime());
                fund.setAutoDepositDayOfWeek(request.getAutoDepositDayOfWeek());
                fund.setAutoDepositDayOfMonth(request.getAutoDepositDayOfMonth());
                fund.setAutoDepositMonth(request.getAutoDepositMonth());
                fund.setAutoDepositDay(request.getAutoDepositDay());
                fund.setAutoDepositAmount(request.getAutoDepositAmount());
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

        if (!fund.getOwner().getUserId().equals(userId)) {
            throw new RuntimeException("Chỉ chủ quỹ mới được xóa quỹ");
        }

        // Xóa thành viên trước
        fundMemberRepository.deleteByFund_FundId(fundId);

        // Xóa quỹ
        fundRepository.delete(fund);
    }

    @Override
    @Transactional
    public FundResponse depositToFund(Long userId, Long fundId, BigDecimal amount) {
        Fund fund = fundRepository.findByIdWithRelations(fundId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy quỹ"));

        // Kiểm tra quyền
        if (!fund.getOwner().getUserId().equals(userId) &&
                !fundMemberRepository.existsByFund_FundIdAndUser_UserId(fundId, userId)) {
            throw new RuntimeException("Bạn không có quyền nạp tiền vào quỹ này");
        }

        if (fund.getStatus() != FundStatus.ACTIVE) {
            throw new RuntimeException("Không thể nạp tiền vào quỹ đã đóng");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Số tiền nạp phải lớn hơn 0");
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

        // Trừ ví nguồn, cộng ví đích
        sourceWallet.setBalance(sourceWallet.getBalance().subtract(amount));
        targetWallet.setBalance(targetWallet.getBalance().add(amount));
        walletRepository.save(sourceWallet);
        walletRepository.save(targetWallet);

        // Cập nhật số tiền quỹ
        fund.setCurrentAmount(fund.getCurrentAmount().add(amount));

        // Nếu là quỹ có kỳ hạn và đã đạt mục tiêu, chỉ đánh dấu trạng thái COMPLETED
        // nhưng KHÔNG tự động rút tiền về ví nguồn. Việc rút sẽ do người dùng thực hiện
        // thủ công qua flow "Rút toàn bộ về ví nguồn" trên UI.
        if (fund.getHasDeadline() && fund.getTargetAmount() != null &&
                fund.getCurrentAmount().compareTo(fund.getTargetAmount()) >= 0) {
            fund.setStatus(FundStatus.COMPLETED);
        }

        fund = fundRepository.save(fund);
        return buildFundResponse(fund);
    }

    @Override
    @Transactional
    public FundResponse withdrawFromFund(Long userId, Long fundId, BigDecimal amount) {
        Fund fund = fundRepository.findByIdWithRelations(fundId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy quỹ"));

        // Chỉ quỹ không kỳ hạn mới được rút
        if (fund.getHasDeadline()) {
            throw new RuntimeException("Chỉ quỹ không kỳ hạn mới được rút tiền");
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

        // Trừ số tiền quỹ
        fund.setCurrentAmount(fund.getCurrentAmount().subtract(amount));

        // Trừ số dư ví đích
        Wallet targetWallet = walletRepository.findByIdWithLock(fund.getTargetWallet().getWalletId())
                .orElseThrow(() -> new RuntimeException("Ví đích không tồn tại"));
        targetWallet.setBalance(targetWallet.getBalance().subtract(amount));
        walletRepository.save(targetWallet);

        fund = fundRepository.save(fund);
        return buildFundResponse(fund);
    }

    /**
     * Tất toán quỹ có kỳ hạn khi đạt mục tiêu và chuyển toàn bộ tiền từ ví quỹ
     * về ví nguồn (sourceWallet).
     */
    @Transactional
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
            // Quỹ không kỳ hạn: startDate có thể null
            if (request.getStartDate() != null && request.getStartDate().isBefore(today)) {
                throw new RuntimeException("Ngày bắt đầu phải lớn hơn hoặc bằng ngày hiện tại");
            }
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

