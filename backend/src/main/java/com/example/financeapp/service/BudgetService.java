package com.example.financeapp.service;

import com.example.financeapp.dto.BudgetDTO;
import com.example.financeapp.dto.BudgetRequest;
import com.example.financeapp.entity.Budget;
import com.example.financeapp.entity.Transaction;
import com.example.financeapp.repository.BudgetRepository;
import com.example.financeapp.repository.TransactionRepository;
import com.example.financeapp.repository.UserRepository;
import com.example.financeapp.repository.WalletRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    public BudgetDTO createBudget(Long userId, BudgetRequest request) {
        Budget b = new Budget();
        b.setName(request.getName());
        b.setAmount(request.getAmount());
        b.setStartDate(request.getStartDate());
        b.setEndDate(request.getEndDate());
        b.setCategory(request.getCategory());
        b.setUser(userRepository.findById(userId).orElseThrow());
        b.setWallet(walletRepository.findById(request.getWalletId()).orElse(null));

        budgetRepository.save(b);
        return mapToDTO(b);
    }

    public List<BudgetDTO> getBudgets(Long userId) {
        return budgetRepository.findByUserId(userId)
                .stream().map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private BudgetDTO mapToDTO(Budget budget) {
        BudgetDTO dto = new BudgetDTO();
        dto.setId(budget.getId());
        dto.setName(budget.getName());
        dto.setAmount(budget.getAmount());
        dto.setStartDate(budget.getStartDate());
        dto.setEndDate(budget.getEndDate());
        dto.setWalletId(budget.getWallet() != null ? budget.getWallet().getId() : null);
        dto.setCategory(budget.getCategory());

        // Tính tổng đã chi trong kỳ
        List<Transaction> transactions = transactionRepository
                .findByUserIdAndDateBetween(
                        budget.getUser().getId(),
                        budget.getStartDate(),
                        budget.getEndDate()
                );

        BigDecimal total = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        dto.setTotalSpending(total);

        // % mức độ sử dụng
        dto.setUsagePercent(
                total.doubleValue() / budget.getAmount().doubleValue() * 100
        );

        return dto;
    }
}
