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

    private Transaction createTransaction(Long userId, CreateTransactionRequest req, String typeName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        Wallet wallet = walletRepository.findByWalletIdAndUser_UserId(req.getWalletId(), userId)
                .orElseThrow(() -> new RuntimeException("Ví không tồn tại hoặc không thuộc về bạn"));

        TransactionType type = typeRepository.findByTypeName(typeName)
                .orElseThrow(() -> new RuntimeException("Loại giao dịch không tồn tại"));

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));

        if (!category.getTransactionType().getTypeId().equals(type.getTypeId())) {
            throw new RuntimeException("Danh mục không thuộc loại giao dịch này");
        }

        Transaction tx = new Transaction();
        tx.setUser(user);
        tx.setWallet(wallet);
        tx.setTransactionType(type);
        tx.setCategory(category);
        tx.setAmount(req.getAmount());
        tx.setTransactionDate(req.getTransactionDate());
        tx.setNote(req.getNote());
        tx.setImageUrl(req.getImageUrl());

        // Cập nhật số dư ví
        if ("Chi tiêu".equals(typeName)) {
            wallet.setBalance(wallet.getBalance().subtract(req.getAmount()));
        } else {
            wallet.setBalance(wallet.getBalance().add(req.getAmount()));
        }

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