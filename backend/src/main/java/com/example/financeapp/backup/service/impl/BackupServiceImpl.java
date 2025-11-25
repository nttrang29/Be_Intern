package com.example.financeapp.backup.service.impl;

import com.example.financeapp.backup.dto.BackupHistoryResponse;
import com.example.financeapp.backup.dto.BackupPayload;
import com.example.financeapp.backup.entity.BackupHistory;
import com.example.financeapp.backup.entity.BackupStatus;
import com.example.financeapp.backup.repository.BackupHistoryRepository;
import com.example.financeapp.backup.service.BackupService;
import com.example.financeapp.budget.dto.BudgetResponse;
import com.example.financeapp.budget.service.BudgetService;
import com.example.financeapp.cloud.storage.CloudStorageService;
import com.example.financeapp.transaction.entity.Transaction;
import com.example.financeapp.transaction.repository.TransactionRepository;
import com.example.financeapp.user.entity.User;
import com.example.financeapp.user.repository.UserRepository;
import com.example.financeapp.wallet.entity.Wallet;
import com.example.financeapp.wallet.repository.WalletRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BackupServiceImpl implements BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupServiceImpl.class);
    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Autowired
    private BackupHistoryRepository backupHistoryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private BudgetService budgetService;
    @Autowired
    private CloudStorageService cloudStorageService;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional
    public BackupHistoryResponse triggerBackup(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (!cloudStorageService.isEnabled()) {
            throw new RuntimeException("Chức năng backup cloud đang tắt. Vui lòng cấu hình cloud.aws.*");
        }

        BackupHistory history = new BackupHistory();
        history.setUser(user);
        history.setStatus(BackupStatus.PENDING);
        history.setRequestedAt(LocalDateTime.now());
        history = backupHistoryRepository.save(history);

        try {
            BackupPayload payload = buildPayload(user);
            byte[] jsonData = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(payload);

            String key = buildFileKey(user.getUserId());
            String url = cloudStorageService.uploadFile(key, jsonData, "application/json");

            history.setStatus(BackupStatus.SUCCESS);
            history.setFileKey(key);
            history.setFileUrl(url);
            history.setFileSizeBytes((long) jsonData.length);
            history.setCompletedAt(LocalDateTime.now());
            history.setErrorMessage(null);

            log.info("Backup thành công cho user {} -> {}", user.getEmail(), url);
        } catch (Exception e) {
            history.setStatus(BackupStatus.FAILED);
            history.setCompletedAt(LocalDateTime.now());
            history.setErrorMessage(e.getMessage());
            log.error("Backup thất bại cho user {}: {}", user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Backup dữ liệu thất bại: " + e.getMessage(), e);
        } finally {
            history = backupHistoryRepository.save(history);
        }

        return BackupHistoryResponse.fromEntity(history);
    }

    @Override
    public List<BackupHistoryResponse> getBackupHistory(Long userId) {
        return backupHistoryRepository.findByUser_UserIdOrderByRequestedAtDesc(userId)
                .stream()
                .map(BackupHistoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private BackupPayload buildPayload(User user) {
        BackupPayload payload = new BackupPayload();

        BackupPayload.BackupUser backupUser = new BackupPayload.BackupUser();
        backupUser.setUserId(user.getUserId());
        backupUser.setFullName(user.getFullName());
        backupUser.setEmail(user.getEmail());
        backupUser.setCreatedAt(user.getCreatedAt());
        payload.setUser(backupUser);

        List<Wallet> wallets = walletRepository.findByUser_UserId(user.getUserId());
        payload.setWallets(wallets.stream().map(wallet -> {
            BackupPayload.BackupWallet dto = new BackupPayload.BackupWallet();
            dto.setWalletId(wallet.getWalletId());
            dto.setWalletName(wallet.getWalletName());
            dto.setCurrencyCode(wallet.getCurrencyCode());
            dto.setBalance(wallet.getBalance());
            dto.setDefault(wallet.isDefault());
            dto.setDescription(wallet.getDescription());
            return dto;
        }).collect(Collectors.toList()));

        List<Transaction> transactions = transactionRepository.findByUser_UserIdOrderByTransactionDateDesc(user.getUserId());
        payload.setTransactions(transactions.stream().map(tx -> {
            BackupPayload.BackupTransaction dto = new BackupPayload.BackupTransaction();
            dto.setTransactionId(tx.getTransactionId());
            dto.setWalletId(tx.getWallet().getWalletId());
            dto.setWalletName(tx.getWallet().getWalletName());
            dto.setTransactionType(tx.getTransactionType().getTypeName());
            dto.setCategoryName(tx.getCategory().getCategoryName());
            dto.setAmount(tx.getAmount());
            dto.setTransactionDate(tx.getTransactionDate());
            dto.setNote(tx.getNote());
            return dto;
        }).collect(Collectors.toList()));

        List<BudgetResponse> budgets = budgetService.getAllBudgets(user.getUserId());
        payload.setBudgets(budgets.stream().map(budget -> {
            BackupPayload.BackupBudget dto = new BackupPayload.BackupBudget();
            dto.setBudgetId(budget.getBudgetId());
            dto.setCategoryName(budget.getCategoryName());
            dto.setWalletName(budget.getWalletName());
            dto.setAmountLimit(budget.getAmountLimit());
            dto.setSpentAmount(budget.getSpentAmount());
            dto.setRemainingAmount(budget.getRemainingAmount());
            dto.setStatus(budget.getStatus());
            dto.setBudgetStatus(budget.getBudgetStatus());
            return dto;
        }).collect(Collectors.toList()));

        return payload;
    }

    private String buildFileKey(Long userId) {
        String timestamp = LocalDateTime.now().format(FILE_NAME_FORMATTER);
        return String.format("user-%d/backup-%s.json", userId, timestamp);
    }
}

