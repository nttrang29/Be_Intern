package com.example.financeapp.scheduledtransaction.service.impl;

import com.example.financeapp.category.entity.Category;
import com.example.financeapp.category.repository.CategoryRepository;
import com.example.financeapp.email.EmailService;
import com.example.financeapp.notification.entity.Notification;
import com.example.financeapp.notification.repository.NotificationRepository;
import com.example.financeapp.scheduledtransaction.dto.CreateScheduledTransactionRequest;
import com.example.financeapp.scheduledtransaction.dto.ScheduledTransactionLogResponse;
import com.example.financeapp.scheduledtransaction.dto.ScheduledTransactionResponse;
import com.example.financeapp.scheduledtransaction.entity.ScheduledTransaction;
import com.example.financeapp.scheduledtransaction.entity.ScheduledTransactionLog;
import com.example.financeapp.scheduledtransaction.entity.ScheduleStatus;
import com.example.financeapp.scheduledtransaction.entity.ScheduleType;
import com.example.financeapp.scheduledtransaction.repository.ScheduledTransactionLogRepository;
import com.example.financeapp.scheduledtransaction.repository.ScheduledTransactionRepository;
import com.example.financeapp.scheduledtransaction.service.ScheduledTransactionService;
import com.example.financeapp.transaction.dto.CreateTransactionRequest;
import com.example.financeapp.transaction.entity.TransactionType;
import com.example.financeapp.transaction.repository.TransactionTypeRepository;
import com.example.financeapp.transaction.service.TransactionService;
import com.example.financeapp.user.entity.User;
import com.example.financeapp.user.repository.UserRepository;
import com.example.financeapp.wallet.entity.Wallet;
import com.example.financeapp.wallet.repository.WalletRepository;
import com.example.financeapp.wallet.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

@Service
public class ScheduledTransactionServiceImpl implements ScheduledTransactionService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTransactionServiceImpl.class);

    @Autowired
    private ScheduledTransactionRepository scheduledTransactionRepository;

    @Autowired
    private ScheduledTransactionLogRepository scheduledTransactionLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionTypeRepository transactionTypeRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EmailService emailService;

    @Override
    @Transactional
    public ScheduledTransactionResponse createScheduledTransaction(
            Long userId, CreateScheduledTransactionRequest request) {

        // 1. Kiểm tra user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // 2. Kiểm tra wallet và quyền truy cập
        Wallet wallet = walletRepository.findById(request.getWalletId())
                .orElseThrow(() -> new RuntimeException("Ví không tồn tại"));

        if (!walletService.hasAccess(wallet.getWalletId(), userId)) {
            throw new RuntimeException("Bạn không có quyền truy cập ví này");
        }

        // 3. Kiểm tra transaction type
        TransactionType transactionType = transactionTypeRepository.findById(request.getTransactionTypeId())
                .orElseThrow(() -> new RuntimeException("Loại giao dịch không tồn tại"));

        // 4. Kiểm tra category
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));

        if (!category.getTransactionType().getTypeId().equals(transactionType.getTypeId())) {
            throw new RuntimeException("Danh mục không thuộc loại giao dịch này");
        }

        // 5. Validate amount
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Số tiền phải lớn hơn 0");
        }

        // 6. Validate dates
        validateDates(request);

        // 7. Validate schedule type specific fields
        validateScheduleTypeFields(request);

        // 8. Tạo scheduled transaction
        ScheduledTransaction scheduled = new ScheduledTransaction();
        scheduled.setUser(user);
        scheduled.setWallet(wallet);
        scheduled.setTransactionType(transactionType);
        scheduled.setCategory(category);
        scheduled.setAmount(request.getAmount());
        scheduled.setNote(request.getNote());
        scheduled.setScheduleType(request.getScheduleType());
        scheduled.setStatus(ScheduleStatus.PENDING);
        scheduled.setExecutionTime(request.getExecutionTime());
        scheduled.setEndDate(request.getEndDate());

        // Set các trường theo schedule type
        scheduled.setDayOfWeek(request.getDayOfWeek());
        scheduled.setDayOfMonth(request.getDayOfMonth());
        scheduled.setMonth(request.getMonth());
        scheduled.setDay(request.getDay());

        // 9. Tính nextExecutionDate
        scheduled.setNextExecutionDate(calculateInitialNextExecutionDate(request));

        scheduled = scheduledTransactionRepository.save(scheduled);

        return ScheduledTransactionResponse.fromEntity(scheduled);
    }

    @Override
    public List<ScheduledTransactionResponse> getAllScheduledTransactions(Long userId) {
        List<ScheduledTransaction> scheduledTransactions =
                scheduledTransactionRepository.findByUser_UserIdOrderByNextExecutionDateAsc(userId);

        return scheduledTransactions.stream()
                .map(ScheduledTransactionResponse::fromEntity)
                .toList();
    }

    @Override
    public ScheduledTransactionResponse getScheduledTransactionById(Long userId, Long scheduleId) {
        ScheduledTransaction scheduled = scheduledTransactionRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch giao dịch"));

        if (!scheduled.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xem lịch giao dịch này");
        }

        return ScheduledTransactionResponse.fromEntity(scheduled);
    }

    @Override
    @Transactional
    public void deleteScheduledTransaction(Long userId, Long scheduleId) {
        ScheduledTransaction scheduled = scheduledTransactionRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch giao dịch"));

        if (!scheduled.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa lịch giao dịch này");
        }

        scheduledTransactionRepository.delete(scheduled);
    }

    @Override
    @Transactional
    public ScheduledTransactionResponse cancelScheduledTransaction(Long userId, Long scheduleId) {
        ScheduledTransaction scheduled = scheduledTransactionRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch giao dịch"));

        if (!scheduled.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền hủy lịch giao dịch này");
        }

        // Kiểm tra nếu đã hủy rồi
        if (scheduled.getStatus() == ScheduleStatus.CANCELLED) {
            throw new RuntimeException("Lịch giao dịch này đã được hủy trước đó");
        }

        // Kiểm tra nếu đã hoàn thành
        if (scheduled.getStatus() == ScheduleStatus.COMPLETED) {
            throw new RuntimeException("Không thể hủy lịch giao dịch đã hoàn thành");
        }

        // Đổi status thành CANCELLED
        scheduled.setStatus(ScheduleStatus.CANCELLED);
        scheduled = scheduledTransactionRepository.save(scheduled);

        return ScheduledTransactionResponse.fromEntity(scheduled);
    }

    @Override
    @Transactional
    public ScheduledTransactionResponse restartScheduledTransaction(Long userId, Long scheduleId, CreateScheduledTransactionRequest request) {
        ScheduledTransaction scheduled = scheduledTransactionRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch giao dịch"));

        // Kiểm tra quyền: user phải là owner của schedule HOẶC có quyền truy cập wallet của schedule
        boolean isOwner = scheduled.getUser().getUserId().equals(userId);
        boolean hasWalletAccess = walletService.hasAccess(scheduled.getWallet().getWalletId(), userId);

        if (!isOwner && !hasWalletAccess) {
            throw new RuntimeException("Bạn không có quyền khởi động lại lịch giao dịch này");
        }

        // Chỉ cho phép restart các schedule đã FAILED
        if (scheduled.getStatus() != ScheduleStatus.FAILED) {
            throw new RuntimeException("Chỉ có thể khởi động lại các lịch giao dịch đã thất bại");
        }

        // Cập nhật thông tin từ request (chủ yếu là thời gian mới)
        // Validate và lấy wallet mới (nếu có thay đổi)
        Wallet wallet = walletRepository.findById(request.getWalletId())
                .orElseThrow(() -> new RuntimeException("Ví không tồn tại"));

        // Kiểm tra quyền truy cập wallet (bao gồm cả ví được chia sẻ)
        if (!walletService.hasAccess(wallet.getWalletId(), userId)) {
            throw new RuntimeException("Bạn không có quyền truy cập ví này");
        }

        // Validate category
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));

        // Validate TransactionType
        TransactionType transactionType = transactionTypeRepository.findById(request.getTransactionTypeId())
                .orElseThrow(() -> new RuntimeException("Loại giao dịch không hợp lệ"));

        // Cập nhật scheduled transaction
        scheduled.setWallet(wallet);
        scheduled.setCategory(category);
        scheduled.setTransactionType(transactionType);
        scheduled.setAmount(request.getAmount());
        scheduled.setNote(request.getNote());
        scheduled.setScheduleType(request.getScheduleType());
        scheduled.setExecutionTime(request.getExecutionTime());
        scheduled.setEndDate(request.getEndDate());

        // Set các trường theo schedule type
        scheduled.setDayOfWeek(request.getDayOfWeek());
        scheduled.setDayOfMonth(request.getDayOfMonth());
        scheduled.setMonth(request.getMonth());
        scheduled.setDay(request.getDay());

        // Reset status về PENDING
        scheduled.setStatus(ScheduleStatus.PENDING);

        // Tính nextExecutionDate mới
        scheduled.setNextExecutionDate(calculateInitialNextExecutionDate(request));

        // Giữ nguyên logs cũ, không reset completedCount và failedCount

        scheduled = scheduledTransactionRepository.save(scheduled);

        return ScheduledTransactionResponse.fromEntity(scheduled);
    }

    @Override
    @Transactional
    public void executeScheduledTransaction(ScheduledTransaction scheduled) {
        BigDecimal walletBalanceBefore = BigDecimal.ZERO;
        BigDecimal walletBalanceAfter = BigDecimal.ZERO;

        try {
            Wallet wallet = walletRepository.findByIdWithLock(scheduled.getWallet().getWalletId())
                    .orElseThrow(() -> new RuntimeException("Ví không tồn tại"));

            walletBalanceBefore = wallet.getBalance();

            // Kiểm tra nếu là chi tiêu, cần kiểm tra số dư
            if ("Chi tiêu".equals(scheduled.getTransactionType().getTypeName())) {
                if (wallet.getBalance().compareTo(scheduled.getAmount()) < 0) {
                    // Không đủ tiền - tạo log thất bại
                    BigDecimal shortage = scheduled.getAmount().subtract(wallet.getBalance());
                    String failMessage = String.format("Không đủ số dư. Cần thêm %s VND",
                            formatMoney(shortage));

                    createExecutionLog(scheduled, ScheduledTransactionLog.LogStatus.FAILED,
                            failMessage, walletBalanceBefore, walletBalanceBefore);

                    // Gửi notification và email
                    sendInsufficientBalanceNotification(scheduled, wallet, shortage);
                    sendInsufficientBalanceEmail(scheduled, wallet, shortage);

                    scheduled.setFailedCount(scheduled.getFailedCount() + 1);

                    // Nếu là định kỳ, vẫn tính nextExecutionDate cho lần tiếp theo
                    if (scheduled.getScheduleType() != ScheduleType.ONCE) {
                        LocalDate nextDate = calculateNextExecutionDate(scheduled);
                        if (scheduled.getEndDate() != null && nextDate != null && nextDate.isAfter(scheduled.getEndDate())) {
                            scheduled.setStatus(ScheduleStatus.COMPLETED);
                            scheduled.setNextExecutionDate(null);
                        } else if (nextDate != null) {
                            scheduled.setNextExecutionDate(nextDate);
                            scheduled.setStatus(ScheduleStatus.PENDING);
                        } else {
                            scheduled.setStatus(ScheduleStatus.COMPLETED);
                        }
                    } else {
                        scheduled.setStatus(ScheduleStatus.FAILED);
                    }

                    scheduledTransactionRepository.save(scheduled);
                    return;
                }
            }

            // Tạo transaction thực tế
            CreateTransactionRequest txRequest = new CreateTransactionRequest();
            txRequest.setWalletId(scheduled.getWallet().getWalletId());
            txRequest.setCategoryId(scheduled.getCategory().getCategoryId());
            txRequest.setAmount(scheduled.getAmount());
            txRequest.setNote(scheduled.getNote() != null
                    ? scheduled.getNote() + " [Tự động từ lịch hẹn #" + scheduled.getScheduleId() + "]"
                    : "[Tự động từ lịch hẹn #" + scheduled.getScheduleId() + "]");
            txRequest.setTransactionDate(LocalDateTime.of(
                    scheduled.getNextExecutionDate(),
                    scheduled.getExecutionTime()));

            // Tạo transaction thực tế
            if ("Chi tiêu".equals(scheduled.getTransactionType().getTypeName())) {
                transactionService.createExpense(scheduled.getUser().getUserId(), txRequest);
            } else {
                transactionService.createIncome(scheduled.getUser().getUserId(), txRequest);
            }

            // Lấy số dư mới sau giao dịch
            Wallet updatedWallet = walletRepository.findById(scheduled.getWallet().getWalletId())
                    .orElse(wallet);
            walletBalanceAfter = updatedWallet.getBalance();

            // Tạo log thành công
            String successMessage = String.format("Giao dịch %s %s VND thành công",
                    "Chi tiêu".equals(scheduled.getTransactionType().getTypeName()) ? "chi" : "thu",
                    formatMoney(scheduled.getAmount()));
            createExecutionLog(scheduled, ScheduledTransactionLog.LogStatus.COMPLETED,
                    successMessage, walletBalanceBefore, walletBalanceAfter);

            // Cập nhật scheduled transaction
            scheduled.setCompletedCount(scheduled.getCompletedCount() + 1);

            // Nếu là ONCE, đánh dấu COMPLETED
            if (scheduled.getScheduleType() == ScheduleType.ONCE) {
                scheduled.setStatus(ScheduleStatus.COMPLETED);
            } else {
                // Định kỳ: tính nextExecutionDate và quay về PENDING
                LocalDate nextDate = calculateNextExecutionDate(scheduled);

                // Kiểm tra xem có vượt quá endDate không
                if (scheduled.getEndDate() != null && nextDate != null && nextDate.isAfter(scheduled.getEndDate())) {
                    // Đã hết hạn, đánh dấu COMPLETED
                    scheduled.setStatus(ScheduleStatus.COMPLETED);
                    scheduled.setNextExecutionDate(null);
                } else if (nextDate != null) {
                    // Còn tiếp tục
                    scheduled.setNextExecutionDate(nextDate);
                    scheduled.setStatus(ScheduleStatus.PENDING);
                } else {
                    // Không có lần tiếp theo
                    scheduled.setStatus(ScheduleStatus.COMPLETED);
                }
            }

            scheduledTransactionRepository.save(scheduled);

            log.info("Đã thực hiện thành công scheduled transaction #{} - {} {} VND",
                    scheduled.getScheduleId(),
                    scheduled.getTransactionType().getTypeName(),
                    formatMoney(scheduled.getAmount()));

        } catch (Exception e) {
            // Lỗi khi thực hiện
            String errorMessage = "Lỗi: " + e.getMessage();
            createExecutionLog(scheduled, ScheduledTransactionLog.LogStatus.FAILED,
                    errorMessage, walletBalanceBefore, walletBalanceBefore);

            scheduled.setStatus(ScheduleStatus.FAILED);
            scheduled.setFailedCount(scheduled.getFailedCount() + 1);
            scheduledTransactionRepository.save(scheduled);

            log.error("Lỗi khi thực hiện scheduled transaction #{}: {}",
                    scheduled.getScheduleId(), e.getMessage());
            throw e;
        }
    }

    /**
     * Tạo log thực hiện scheduled transaction
     */
    private void createExecutionLog(ScheduledTransaction scheduled,
                                    ScheduledTransactionLog.LogStatus status,
                                    String message,
                                    BigDecimal balanceBefore,
                                    BigDecimal balanceAfter) {
        ScheduledTransactionLog log = new ScheduledTransactionLog();
        log.setScheduledTransaction(scheduled);
        log.setStatus(status);
        log.setMessage(message);
        log.setAmount(scheduled.getAmount());
        log.setWalletBalanceBefore(balanceBefore);
        log.setWalletBalanceAfter(balanceAfter);
        log.setExecutionTime(LocalDateTime.now());
        scheduledTransactionLogRepository.save(log);
    }

    /**
     * Gửi notification khi không đủ số dư
     */
    private void sendInsufficientBalanceNotification(ScheduledTransaction scheduled,
                                                     Wallet wallet, BigDecimal shortage) {
        try {
            Notification notification = new Notification();
            notification.setUser(scheduled.getUser());
            notification.setReceiverRole("USER");
            notification.setType(Notification.NotificationType.SCHEDULED_TRANSACTION_FAILED);
            notification.setTitle("⚠️ Giao dịch tự động thất bại");
            notification.setMessage(String.format(
                    "Lịch hẹn \"%s\" không thể thực hiện do ví \"%s\" không đủ số dư. Cần thêm %s VND.",
                    scheduled.getCategory().getCategoryName(),
                    wallet.getWalletName(),
                    formatMoney(shortage)
            ));
            notification.setReferenceId(scheduled.getScheduleId());
            notification.setReferenceType("SCHEDULED_TRANSACTION");
            notification.setIsRead(false);
            notificationRepository.save(notification);

            log.info("Đã gửi notification thất bại cho user {} - scheduled transaction #{}",
                    scheduled.getUser().getEmail(), scheduled.getScheduleId());
        } catch (Exception e) {
            log.error("Không thể gửi notification: {}", e.getMessage());
        }
    }

    /**
     * Gửi email nhắc nhở khi không đủ số dư
     */
    private void sendInsufficientBalanceEmail(ScheduledTransaction scheduled,
                                              Wallet wallet, BigDecimal shortage) {
        try {
            String subject = "[MyWallet] ⚠️ Giao dịch tự động thất bại";
            String content = String.format(
                    "Xin chào %s,\n\n" +
                            "Lịch hẹn giao dịch tự động của bạn không thể thực hiện:\n\n" +
                            "📋 Thông tin lịch hẹn:\n" +
                            "   • Danh mục: %s (%s)\n" +
                            "   • Ví: %s\n" +
                            "   • Số tiền: %s VND\n\n" +
                            "⚠️ Lý do thất bại:\n" +
                            "   Ví không đủ số dư. Số dư hiện tại: %s VND\n" +
                            "   Cần thêm: %s VND\n\n" +
                            "💡 Vui lòng nạp thêm tiền vào ví để giao dịch có thể thực hiện ở lần chạy tiếp theo.\n\n" +
                            "Trân trọng,\nĐội ngũ MyWallet",
                    scheduled.getUser().getFullName(),
                    scheduled.getCategory().getCategoryName(),
                    scheduled.getTransactionType().getTypeName(),
                    wallet.getWalletName(),
                    formatMoney(scheduled.getAmount()),
                    formatMoney(wallet.getBalance()),
                    formatMoney(shortage)
            );

            emailService.sendEmail(scheduled.getUser().getEmail(), subject, content);

            log.info("Đã gửi email nhắc nhở cho user {} - scheduled transaction #{}",
                    scheduled.getUser().getEmail(), scheduled.getScheduleId());
        } catch (Exception e) {
            log.error("Không thể gửi email: {}", e.getMessage());
        }
    }

    /**
     * Format số tiền theo định dạng Việt Nam
     */
    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0";
        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        return formatter.format(amount);
    }

    /**
     * Lấy lịch sử thực hiện của scheduled transaction
     */
    public List<ScheduledTransactionLogResponse> getExecutionLogs(Long userId, Long scheduleId) {
        // Kiểm tra quyền truy cập
        ScheduledTransaction scheduled = scheduledTransactionRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch giao dịch"));

        if (!scheduled.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xem lịch sử này");
        }

        List<ScheduledTransactionLog> logs = scheduledTransactionLogRepository
                .findByScheduledTransaction_ScheduleIdOrderByExecutionTimeDesc(scheduleId);

        return logs.stream()
                .map(ScheduledTransactionLogResponse::fromEntity)
                .toList();
    }

    @Override
    public LocalDate previewNextExecutionDate(CreateScheduledTransactionRequest request) {
        // Validate cơ bản trước khi tính
        if (request.getStartDate() == null || request.getExecutionTime() == null) {
            return null;
        }

        // Tính nextExecutionDate tương tự như calculateInitialNextExecutionDate
        return calculateInitialNextExecutionDate(request);
    }

    @Override
    public LocalDate calculateNextExecutionDate(ScheduledTransaction scheduled) {
        LocalDate current = scheduled.getNextExecutionDate();
        if (current == null) {
            current = LocalDate.now();
        }

        switch (scheduled.getScheduleType()) {
            case ONCE:
                return null; // Không có lần tiếp theo

            case DAILY:
                return current.plusDays(1);

            case WEEKLY:
                if (scheduled.getDayOfWeek() == null) {
                    return current.plusWeeks(1);
                }
                // Tìm thứ tiếp theo
                int currentDayOfWeek = current.getDayOfWeek().getValue();
                int targetDayOfWeek = scheduled.getDayOfWeek();
                int daysToAdd = targetDayOfWeek - currentDayOfWeek;
                if (daysToAdd <= 0) {
                    daysToAdd += 7; // Tuần sau
                }
                return current.plusDays(daysToAdd);

            case MONTHLY:
                if (scheduled.getDayOfMonth() == null) {
                    return current.plusMonths(1);
                }
                // Tìm ngày tiếp theo trong tháng
                LocalDate next = current.plusMonths(1);
                int dayOfMonth = scheduled.getDayOfMonth();
                // Đảm bảo ngày hợp lệ trong tháng
                int maxDay = next.lengthOfMonth();
                int actualDay = Math.min(dayOfMonth, maxDay);
                return next.withDayOfMonth(actualDay);

            case YEARLY:
                if (scheduled.getMonth() == null || scheduled.getDay() == null) {
                    return current.plusYears(1);
                }
                // Tìm ngày tiếp theo trong năm
                LocalDate nextYear = current.plusYears(1);
                int month = scheduled.getMonth();
                int day = scheduled.getDay();
                // Đảm bảo ngày hợp lệ
                try {
                    return nextYear.withMonth(month).withDayOfMonth(
                            Math.min(day, nextYear.withMonth(month).lengthOfMonth()));
                } catch (Exception e) {
                    return nextYear.plusYears(1).withMonth(month).withDayOfMonth(
                            Math.min(day, nextYear.plusYears(1).withMonth(month).lengthOfMonth()));
                }

            default:
                return current.plusDays(1);
        }
    }

    /**
     * Tính ngày thực hiện đầu tiên dựa trên request
     */
    private LocalDate calculateInitialNextExecutionDate(CreateScheduledTransactionRequest request) {
        switch (request.getScheduleType()) {
            case ONCE:
                return request.getStartDate();

            case DAILY:
                // Bắt đầu từ startDate, nhưng nếu đã qua giờ thì bắt đầu từ ngày mai
                LocalDate today = LocalDate.now();
                if (request.getStartDate().isBefore(today) ||
                        (request.getStartDate().equals(today) &&
                                request.getExecutionTime().isBefore(LocalTime.now()))) {
                    return today.plusDays(1);
                }
                return request.getStartDate();

            case WEEKLY:
                if (request.getDayOfWeek() == null) {
                    return request.getStartDate();
                }
                // Tìm thứ gần nhất
                LocalDate start = request.getStartDate();
                int currentDayOfWeek = start.getDayOfWeek().getValue();
                int targetDayOfWeek = request.getDayOfWeek();
                int daysToAdd = targetDayOfWeek - currentDayOfWeek;
                if (daysToAdd < 0) {
                    daysToAdd += 7;
                }
                LocalDate nextDate = start.plusDays(daysToAdd);
                // Nếu đã qua giờ hôm nay, thì tính từ ngày mai
                if (nextDate.equals(LocalDate.now()) &&
                        request.getExecutionTime().isBefore(LocalTime.now())) {
                    nextDate = nextDate.plusDays(7);
                }
                return nextDate;

            case MONTHLY:
                if (request.getDayOfMonth() == null) {
                    return request.getStartDate();
                }
                LocalDate monthlyStart = request.getStartDate();
                int dayOfMonth = request.getDayOfMonth();
                int maxDay = monthlyStart.lengthOfMonth();
                int actualDay = Math.min(dayOfMonth, maxDay);
                LocalDate nextMonthly = monthlyStart.withDayOfMonth(actualDay);
                if (nextMonthly.isBefore(LocalDate.now()) ||
                        (nextMonthly.equals(LocalDate.now()) &&
                                request.getExecutionTime().isBefore(LocalTime.now()))) {
                    nextMonthly = nextMonthly.plusMonths(1);
                    maxDay = nextMonthly.lengthOfMonth();
                    actualDay = Math.min(dayOfMonth, maxDay);
                    nextMonthly = nextMonthly.withDayOfMonth(actualDay);
                }
                return nextMonthly;

            case YEARLY:
                if (request.getMonth() == null || request.getDay() == null) {
                    return request.getStartDate();
                }
                LocalDate yearlyStart = request.getStartDate();
                try {
                    LocalDate nextYearly = yearlyStart.withMonth(request.getMonth())
                            .withDayOfMonth(Math.min(request.getDay(),
                                    yearlyStart.withMonth(request.getMonth()).lengthOfMonth()));
                    if (nextYearly.isBefore(LocalDate.now()) ||
                            (nextYearly.equals(LocalDate.now()) &&
                                    request.getExecutionTime().isBefore(LocalTime.now()))) {
                        nextYearly = nextYearly.plusYears(1);
                        nextYearly = nextYearly.withMonth(request.getMonth())
                                .withDayOfMonth(Math.min(request.getDay(),
                                        nextYearly.withMonth(request.getMonth()).lengthOfMonth()));
                    }
                    return nextYearly;
                } catch (Exception e) {
                    return yearlyStart.plusYears(1);
                }

            default:
                return request.getStartDate();
        }
    }

    /**
     * Validate dates (startDate, endDate)
     */
    private void validateDates(CreateScheduledTransactionRequest request) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // Validate startDate
        if (request.getStartDate() == null) {
            throw new RuntimeException("Vui lòng chọn ngày bắt đầu");
        }

        // Cho ONCE: startDate phải >= today (hoặc >= today + executionTime nếu cùng ngày)
        if (request.getScheduleType() == ScheduleType.ONCE) {
            if (request.getStartDate().isBefore(today)) {
                throw new RuntimeException("Ngày thực hiện không được là ngày trong quá khứ");
            }
            if (request.getStartDate().equals(today) &&
                    request.getExecutionTime() != null &&
                    request.getExecutionTime().isBefore(now)) {
                throw new RuntimeException("Thời gian thực hiện không được là thời gian trong quá khứ");
            }
        }

        // Validate endDate (nếu có)
        if (request.getEndDate() != null) {
            // endDate chỉ áp dụng cho recurring (không phải ONCE)
            if (request.getScheduleType() == ScheduleType.ONCE) {
                throw new RuntimeException("Lịch một lần không cần ngày kết thúc");
            }

            // endDate phải >= startDate
            if (request.getEndDate().isBefore(request.getStartDate())) {
                throw new RuntimeException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
            }
        }
    }

    /**
     * Validate các trường theo schedule type
     */
    private void validateScheduleTypeFields(CreateScheduledTransactionRequest request) {
        switch (request.getScheduleType()) {
            case ONCE:
            case DAILY:
                // Không cần validate thêm trường nào
                break;
            case WEEKLY:
                if (request.getDayOfWeek() == null) {
                    throw new RuntimeException("Vui lòng chọn thứ trong tuần cho lịch hàng tuần");
                }
                break;
            case MONTHLY:
                if (request.getDayOfMonth() == null) {
                    throw new RuntimeException("Vui lòng chọn ngày trong tháng cho lịch hàng tháng");
                }
                break;
            case YEARLY:
                if (request.getMonth() == null || request.getDay() == null) {
                    throw new RuntimeException("Vui lòng chọn tháng và ngày cho lịch hàng năm");
                }
                break;
        }
    }
}

