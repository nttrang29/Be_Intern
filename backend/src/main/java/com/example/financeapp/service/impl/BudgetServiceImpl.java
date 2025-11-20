package com.example.financeapp.service.impl;

import com.example.financeapp.dto.CreateBudgetRequest;
import com.example.financeapp.entity.*;
import com.example.financeapp.repository.*;
import com.example.financeapp.service.BudgetService;
import com.example.financeapp.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        return budgetRepository.save(budget);
    }
}