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
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BudgetServiceImpl implements BudgetService {

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

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new RuntimeException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));

        if (!"Chi tiêu".equals(category.getTransactionType().getTypeName())) {
            throw new RuntimeException("Chỉ được tạo ngân sách cho danh mục Chi tiêu");
        }

        Long walletIdForCheck = null;
        if (request.getWalletId() != null) {
            Wallet wallet = walletRepository.findById(request.getWalletId())
                    .orElseThrow(() -> new RuntimeException("Ví không tồn tại"));

            if (!walletService.hasAccess(wallet.getWalletId(), userId)) {
                throw new RuntimeException("Bạn không có quyền truy cập ví này");
            }
            walletIdForCheck = wallet.getWalletId();
        }

        // KIỂM TRA GIAO NHAU (OVERLAP) – CHẶN HOÀN TOÀN
        boolean hasOverlap = budgetRepository.existsOverlappingBudget(
                user,
                request.getCategoryId(),
                walletIdForCheck,
                request.getStartDate(),
                request.getEndDate()
        );

        if (hasOverlap) {
            String walletInfo = walletIdForCheck == null ? "tất cả ví" : "ví đã chọn";
            throw new RuntimeException(
                    "Không thể tạo ngân sách mới!\n" +
                            "Danh mục \"" + category.getCategoryName() + "\" trong " + walletInfo +
                            " đã có ngân sách đang áp dụng trong khoảng thời gian này.\n" +
                            "Vui lòng chọn khoảng thời gian không giao nhau hoặc chỉnh sửa ngân sách cũ."
            );
        }

        // Nếu không trùng → tạo bình thường
        Wallet wallet = walletIdForCheck != null
                ? walletRepository.findById(walletIdForCheck).orElse(null)
                : null;

        Budget budget = new Budget();
        budget.setUser(user);
        budget.setCategory(category);
        budget.setWallet(wallet);
        budget.setAmountLimit(request.getAmountLimit());
        budget.setStartDate(request.getStartDate());
        budget.setEndDate(request.getEndDate());
        budget.setNote(request.getNote() != null && !request.getNote().trim().isEmpty()
                ? request.getNote().trim() : null);
        budget.setWarningThreshold(request.getWarningThreshold() != null 
                ? request.getWarningThreshold() : 80.0); // Mặc định 80% nếu không có

        return budgetRepository.save(budget);
    }

    @Override
    public List<BudgetResponse> getAllBudgets(Long userId) {
        // Lấy tất cả budgets của user
        List<Budget> budgets = budgetRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);

        // Chuyển đổi sang BudgetResponse với thông tin đã chi
        return budgets.stream()
                .map(budget -> {
                    // Tự động cập nhật status theo thời gian
                    updateBudgetStatus(budget);
                    BigDecimal spentAmount = calculateSpentAmount(budget);
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

        // Tự động cập nhật status theo thời gian
        updateBudgetStatus(budget);

        // Tính số tiền đã chi
        BigDecimal spentAmount = calculateSpentAmount(budget);

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
     * Tự động cập nhật status của budget theo thời gian
     * ACTIVE: trong khoảng thời gian
     * COMPLETED: đã qua ngày kết thúc
     */
    private void updateBudgetStatus(Budget budget) {
        LocalDate today = LocalDate.now();
        
        if (today.isAfter(budget.getEndDate())) {
            // Đã qua ngày kết thúc -> COMPLETED
            if (budget.getStatus() != BudgetStatus.COMPLETED) {
                budget.setStatus(BudgetStatus.COMPLETED);
                budgetRepository.save(budget);
            }
        } else {
            // Trong khoảng thời gian -> ACTIVE
            if (budget.getStatus() != BudgetStatus.ACTIVE) {
                budget.setStatus(BudgetStatus.ACTIVE);
                budgetRepository.save(budget);
            }
        }
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

        // Validation
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new RuntimeException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        }

        // Kiểm tra wallet nếu có
        final Long walletIdForCheck;
        Wallet wallet = null;
        if (request.getWalletId() != null) {
            wallet = walletRepository.findById(request.getWalletId())
                    .orElseThrow(() -> new RuntimeException("Ví không tồn tại"));

            if (!walletService.hasAccess(wallet.getWalletId(), userId)) {
                throw new RuntimeException("Bạn không có quyền truy cập ví này");
            }
            walletIdForCheck = wallet.getWalletId();
        } else {
            walletIdForCheck = null;
        }

        // Lưu categoryId và budgetId để dùng trong lambda
        final Long categoryId = budget.getCategory().getCategoryId();
        final Long finalBudgetId = budgetId;
        final LocalDate newStartDate = request.getStartDate();
        final LocalDate newEndDate = request.getEndDate();

        // KIỂM TRA GIAO NHAU (OVERLAP) – CHẶN HOÀN TOÀN
        // Loại trừ chính budget hiện tại khi kiểm tra overlap
        boolean hasOverlap = budgetRepository.existsOverlappingBudget(
                budget.getUser(),
                categoryId,
                walletIdForCheck,
                newStartDate,
                newEndDate
        );

        // Nếu có overlap, kiểm tra xem có phải chính budget này không
        if (hasOverlap) {
            // Kiểm tra xem có budget khác (không phải budget hiện tại) trùng không
            List<Budget> allBudgets = budgetRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);
            boolean foundOtherOverlap = allBudgets.stream()
                    .filter(b -> !b.getBudgetId().equals(finalBudgetId))
                    .anyMatch(b -> {
                        Long bWalletId = b.getWallet() != null ? b.getWallet().getWalletId() : null;
                        boolean walletMatch = (bWalletId == null && walletIdForCheck == null) ||
                                (bWalletId != null && walletIdForCheck != null && bWalletId.equals(walletIdForCheck));
                        return walletMatch &&
                                b.getCategory().getCategoryId().equals(categoryId) &&
                                !b.getStartDate().isAfter(newEndDate) &&
                                !b.getEndDate().isBefore(newStartDate);
                    });

            if (foundOtherOverlap) {
                throw new RuntimeException("Đã có ngân sách khác trùng khoảng thời gian cho danh mục và ví này");
            }
        }

        // Cập nhật thông tin
        budget.setWallet(wallet);
        budget.setAmountLimit(request.getAmountLimit());
        budget.setStartDate(request.getStartDate());
        budget.setEndDate(request.getEndDate());
        budget.setNote(request.getNote());
        if (request.getWarningThreshold() != null) {
            budget.setWarningThreshold(request.getWarningThreshold());
        }

        // Tự động cập nhật status theo thời gian
        updateBudgetStatus(budget);

        Budget savedBudget = budgetRepository.save(budget);

        // Tính số tiền đã chi
        BigDecimal spentAmount = calculateSpentAmount(savedBudget);

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
}