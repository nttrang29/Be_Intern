package com.example.financeapp.scheduledtransaction.service;

import com.example.financeapp.scheduledtransaction.dto.CreateScheduledTransactionRequest;
import com.example.financeapp.scheduledtransaction.dto.ScheduledTransactionResponse;
import com.example.financeapp.scheduledtransaction.entity.ScheduledTransaction;

import java.util.List;

/**
 * Service để quản lý scheduled transactions
 */
public interface ScheduledTransactionService {
    
    /**
     * Tạo scheduled transaction mới
     */
    ScheduledTransactionResponse createScheduledTransaction(Long userId, CreateScheduledTransactionRequest request);
    
    /**
     * Lấy danh sách scheduled transactions của user
     */
    List<ScheduledTransactionResponse> getAllScheduledTransactions(Long userId);
    
    /**
     * Lấy chi tiết một scheduled transaction
     */
    ScheduledTransactionResponse getScheduledTransactionById(Long userId, Long scheduleId);
    
    /**
     * Xóa scheduled transaction
     */
    void deleteScheduledTransaction(Long userId, Long scheduleId);
    
    /**
     * Thực hiện một scheduled transaction (được gọi bởi scheduler)
     */
    void executeScheduledTransaction(ScheduledTransaction scheduledTransaction);
    
    /**
     * Tính toán ngày thực hiện tiếp theo dựa trên scheduleType
     */
    java.time.LocalDate calculateNextExecutionDate(ScheduledTransaction scheduledTransaction);
}

