package com.example.financeapp.budget.service.impl;

import com.example.financeapp.budget.dto.BudgetResponse;
import com.example.financeapp.budget.dto.CreateBudgetRequest;
import com.example.financeapp.budget.dto.UpdateBudgetRequest;
import com.example.financeapp.budget.entity.Budget;
import com.example.financeapp.budget.entity.BudgetStatus;
import com.example.financeapp.budget.repository.BudgetRepository;
import com.example.financeapp.budget.service.BudgetService;
import com.example.financeapp.category.entity.Category;
import com.example.financeapp.category.repository.CategoryRepository;
import com.example.financeapp.transaction.entity.Transaction;
import com.example.financeapp.transaction.repository.TransactionRepository;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class BudgetServiceImpl implements BudgetService {

    private static final EnumSet<BudgetStatus> BLOCKING_STATUSES =
            EnumSet.of(BudgetStatus.PENDING, BudgetStatus.ACTIVE, BudgetStatus.WARNING, BudgetStatus.EXCEEDED);

    @Autowired
    private BudgetRepository budgetRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private WalletService walletService;
    @Autowired
    private TransactionRepository transactionRepository;

    @Override
    @Transactional
    public Budget createBudget(Long userId, CreateBudgetRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        validateDateRange(request.getStartDate(), request.getEndDate());

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));

        if (!"Chi tiêu".equals(category.getTransactionType().getTypeName())) {
            throw new RuntimeException("Chỉ được tạo ngân sách cho danh mục Chi tiêu");
        }

        Wallet wallet = null;
        Long walletIdForCheck = null;
        if (request.getWalletId() != null) {
            wallet = walletRepository.findById(request.getWalletId())
                    .orElseThrow(() -> new RuntimeException("Ví không tồn tại"));

            if (!walletService.hasAccess(wallet.getWalletId(), userId)) {
                throw new RuntimeException("Bạn không có quyền truy cập ví này");
            }
            walletIdForCheck = wallet.getWalletId();
        }

        ensureNoOverlappingBudgets(
                user,
                category.getCategoryId(),
                walletIdForCheck,
                request.getStartDate(),
                request.getEndDate(),
                null
        );

        Budget budget = new Budget();
        budget.setUser(user);
        budget.setCategory(category);
        budget.setWallet(wallet);
        budget.setAmountLimit(request.getAmountLimit());
        budget.setStartDate(request.getStartDate());
        budget.setEndDate(request.getEndDate());
        budget.setNote(request.getNote() != null && !request.getNote().trim().isEmpty()
                ? request.getNote().trim() : null);
        Double warningThreshold = request.getWarningThreshold() != null
                ? request.getWarningThreshold()
                : 80.0;
        budget.setWarningThreshold(warningThreshold);
        budget.setStatus(determineBudgetStatus(budget, BigDecimal.ZERO));

        return budgetRepository.save(budget);
    }

    @Override
    public List<BudgetResponse> getAllBudgets(Long userId) {
        // Lấy tất cả budgets của user
        List<Budget> budgets = budgetRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);

        // Chuyển đổi sang BudgetResponse với thông tin đã chi
        return budgets.stream()
                .map(budget -> {
                    BigDecimal spentAmount = calculateSpentAmount(budget);
                    syncBudgetStatus(budget, spentAmount);
                    return BudgetResponse.fromBudget(budget, spentAmount);
                })
                .collect(Collectors.toList());
    }

    @Override
    public BudgetResponse getBudgetById(Long userId, Long budgetId) {
        // Lấy budget
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ngân sách"));

        // Kiểm tra quyền sở hữu
        if (!budget.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xem ngân sách này");
        }

        // Tính số tiền đã chi
        BigDecimal spentAmount = calculateSpentAmount(budget);
        syncBudgetStatus(budget, spentAmount);

        // Trả về BudgetResponse
        return BudgetResponse.fromBudget(budget, spentAmount);
    }

    @Override
    public List<Transaction> getBudgetTransactions(Long userId, Long budgetId) {
        // Lấy budget
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ngân sách"));

        // Kiểm tra quyền sở hữu
        if (!budget.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xem giao dịch của ngân sách này");
        }

        // Lấy danh sách transactions thuộc budget
        Long walletId = budget.getWallet() != null ? budget.getWallet().getWalletId() : null;
        
        List<Transaction> transactions = transactionRepository.findTransactionsByBudget(
                budget.getUser().getUserId(),
                budget.getCategory().getCategoryId(),
                walletId,
                budget.getStartDate(),
                budget.getEndDate()
        );

        return transactions;
    }

    /**
     * Tính số tiền đã chi trong budget
     */
    private BigDecimal calculateSpentAmount(Budget budget) {
        Long walletId = budget.getWallet() != null ? budget.getWallet().getWalletId() : null;
        
        BigDecimal spentAmount = transactionRepository.calculateTotalSpent(
                budget.getUser().getUserId(),
                budget.getCategory().getCategoryId(),
                walletId,
                budget.getStartDate(),
                budget.getEndDate()
        );

        // Đảm bảo không âm
        return spentAmount != null ? spentAmount : BigDecimal.ZERO;
    }

    @Override
    @Transactional
    public BudgetResponse updateBudget(Long userId, Long budgetId, UpdateBudgetRequest request) {
        // Lấy budget
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ngân sách"));

        // Kiểm tra quyền sở hữu
        if (!budget.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa ngân sách này");
        }

        validateDateRange(request.getStartDate(), request.getEndDate());

        Long walletIdForCheck = budget.getWallet() != null ? budget.getWallet().getWalletId() : null;
        if (request.getWalletId() != null && !Objects.equals(request.getWalletId(), walletIdForCheck)) {
            throw new RuntimeException("Không thể thay đổi ví nguồn của ngân sách");
        }

        ensureStartDateNotBeforeExistingTransactions(budget, request.getStartDate());

        ensureNoOverlappingBudgets(
                budget.getUser(),
                budget.getCategory().getCategoryId(),
                walletIdForCheck,
                request.getStartDate(),
                request.getEndDate(),
                budget.getBudgetId()
        );

        // Cập nhật thông tin
        budget.setAmountLimit(request.getAmountLimit());
        budget.setStartDate(request.getStartDate());
        budget.setEndDate(request.getEndDate());
        budget.setNote(request.getNote() != null && !request.getNote().trim().isEmpty()
                ? request.getNote().trim() : null);
        if (request.getWarningThreshold() != null) {
            budget.setWarningThreshold(request.getWarningThreshold());
        }

        Budget savedBudget = budgetRepository.save(budget);

        // Tính số tiền đã chi
        BigDecimal spentAmount = calculateSpentAmount(savedBudget);
        syncBudgetStatus(savedBudget, spentAmount);

        return BudgetResponse.fromBudget(savedBudget, spentAmount);
    }

    @Override
    @Transactional
    public void deleteBudget(Long userId, Long budgetId) {
        // Lấy budget
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ngân sách"));

        // Kiểm tra quyền sở hữu
        if (!budget.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa ngân sách này");
        }

        // Xóa budget
        budgetRepository.delete(budget);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new RuntimeException("Ngày bắt đầu và ngày kết thúc không được để trống");
        }
        if (!endDate.isAfter(startDate)) {
            throw new RuntimeException("Ngày kết thúc phải lớn hơn ngày bắt đầu");
        }
    }

    private void ensureNoOverlappingBudgets(User user,
                                            Long categoryId,
                                            Long walletId,
                                            LocalDate startDate,
                                            LocalDate endDate,
                                            Long currentBudgetId) {
        List<Budget> overlaps = budgetRepository.findOverlappingBudgets(
                user,
                categoryId,
                walletId,
                startDate,
                endDate
        );

        for (Budget existing : overlaps) {
            if (currentBudgetId != null && existing.getBudgetId().equals(currentBudgetId)) {
                continue;
            }
            BudgetStatus status = syncBudgetStatus(existing);
            if (BLOCKING_STATUSES.contains(status)) {
                String walletInfo = walletId == null ? "tất cả ví" : "ví đã chọn";
                throw new RuntimeException(
                        "Danh mục \"" + existing.getCategory().getCategoryName() + "\" trong " + walletInfo +
                                " đã có ngân sách (" + status.name() + ") trùng thời gian. Vui lòng chọn khoảng thời gian khác.");
            }
        }
    }

    private void ensureStartDateNotBeforeExistingTransactions(Budget budget, LocalDate newStartDate) {
        if (newStartDate == null) {
            return;
        }

        Long walletId = budget.getWallet() != null ? budget.getWallet().getWalletId() : null;
        LocalDate earliestTransactionDate = transactionRepository.findEarliestTransactionDate(
                budget.getUser().getUserId(),
                budget.getCategory().getCategoryId(),
                walletId
        );

        if (earliestTransactionDate != null && newStartDate.isBefore(earliestTransactionDate)) {
            throw new RuntimeException("Ngày bắt đầu không được nhỏ hơn ngày giao dịch đã phát sinh (" + earliestTransactionDate + ")");
        }
    }

    private BudgetStatus syncBudgetStatus(Budget budget) {
        BigDecimal spentAmount = calculateSpentAmount(budget);
        return syncBudgetStatus(budget, spentAmount);
    }

    private BudgetStatus syncBudgetStatus(Budget budget, BigDecimal spentAmount) {
        BudgetStatus newStatus = determineBudgetStatus(budget, spentAmount);
        if (budget.getStatus() != newStatus) {
            budget.setStatus(newStatus);
            budgetRepository.save(budget);
        }
        return newStatus;
    }

    private BudgetStatus determineBudgetStatus(Budget budget, BigDecimal spentAmount) {
        BigDecimal limit = budget.getAmountLimit() != null ? budget.getAmountLimit() : BigDecimal.ZERO;
        BigDecimal safeSpent = spentAmount != null ? spentAmount : BigDecimal.ZERO;
        LocalDate today = LocalDate.now();
        Double warningThreshold = budget.getWarningThreshold() != null ? budget.getWarningThreshold() : 80.0;

        if (limit.compareTo(BigDecimal.ZERO) > 0 && safeSpent.compareTo(limit) > 0) {
            return BudgetStatus.EXCEEDED;
        }

        if (today.isBefore(budget.getStartDate())) {
            return BudgetStatus.PENDING;
        }

        if (today.isAfter(budget.getEndDate())) {
            return BudgetStatus.COMPLETED;
        }

        double usage = 0.0;
        if (limit.compareTo(BigDecimal.ZERO) > 0) {
            usage = safeSpent
                    .divide(limit, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        if (usage >= warningThreshold) {
            return BudgetStatus.WARNING;
        }

        return BudgetStatus.ACTIVE;
    }
}