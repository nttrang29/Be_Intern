package com.example.financeapp.service.impl;

import com.example.financeapp.dto.CreateTransactionRequest;
import com.example.financeapp.entity.*;
import com.example.financeapp.repository.*;
import com.example.financeapp.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired private TransactionRepository transactionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private TransactionTypeRepository typeRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private WalletMemberRepository walletMemberRepository;

    private Transaction createTransaction(Long userId, CreateTransactionRequest req, String typeName) {
        // 1. Kiểm tra user tồn tại
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // 2. ✅ Kiểm tra wallet tồn tại với PESSIMISTIC LOCK
        // Tránh race condition khi nhiều transactions đồng thời
        Wallet wallet = walletRepository.findByIdWithLock(req.getWalletId())
                .orElseThrow(() -> new RuntimeException("Ví không tồn tại"));

        // 3. Kiểm tra quyền truy cập (hỗ trợ shared wallet)
        // User phải là OWNER hoặc MEMBER của ví mới được tạo transaction
        boolean hasAccess = walletMemberRepository.existsByWallet_WalletIdAndUser_UserId(
                req.getWalletId(), 
                userId
        );
        
        if (!hasAccess) {
            throw new RuntimeException("Bạn không có quyền truy cập ví này");
        }

        // 4. Lấy transaction type
        TransactionType type = typeRepository.findByTypeName(typeName)
                .orElseThrow(() -> new RuntimeException("Loại giao dịch không tồn tại"));

        // 5. Lấy category và validate
        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));

        if (!category.getTransactionType().getTypeId().equals(type.getTypeId())) {
            throw new RuntimeException("Danh mục không thuộc loại giao dịch này");
        }

        // 6. Validate amount
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Số tiền phải lớn hơn 0");
        }

        // 7. Kiểm tra số dư đủ cho chi tiêu
        if ("Chi tiêu".equals(typeName)) {
            BigDecimal newBalance = wallet.getBalance().subtract(req.getAmount());
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException(
                    "Số dư không đủ. Số dư hiện tại: " + wallet.getBalance() + 
                    " " + wallet.getCurrencyCode() + 
                    ", Số tiền chi tiêu: " + req.getAmount() + 
                    " " + wallet.getCurrencyCode()
                );
            }
            wallet.setBalance(newBalance);
        } else {
            // Thu nhập
            wallet.setBalance(wallet.getBalance().add(req.getAmount()));
        }

        // 8. Save wallet với balance mới
        walletRepository.save(wallet);

        // 9. Tạo transaction
        Transaction tx = new Transaction();
        tx.setUser(user);
        tx.setWallet(wallet);
        tx.setTransactionType(type);
        tx.setCategory(category);
        tx.setAmount(req.getAmount());
        tx.setTransactionDate(req.getTransactionDate());
        tx.setNote(req.getNote());
        tx.setImageUrl(req.getImageUrl());

        return transactionRepository.save(tx);
    }

    @Override
    @Transactional
    public Transaction createExpense(Long userId, CreateTransactionRequest request) {
        return createTransaction(userId, request, "Chi tiêu");
    }

    @Override
    @Transactional
    public Transaction createIncome(Long userId, CreateTransactionRequest request) {
        return createTransaction(userId, request, "Thu nhập");
    }
}