package com.example.financeapp.scheduledtransaction.service;

import com.example.financeapp.scheduledtransaction.dto.CreateScheduledTransactionRequest;
import com.example.financeapp.scheduledtransaction.dto.ScheduledTransactionLogResponse;
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
     * Khởi động lại scheduled transaction đã thất bại
     * Cập nhật schedule với thời gian mới, giữ lại logs cũ
     */
    ScheduledTransactionResponse restartScheduledTransaction(Long userId, Long scheduleId, CreateScheduledTransactionRequest request);

    /**
     * Xóa scheduled transaction (xóa hoàn toàn khỏi database)
     */
    void deleteScheduledTransaction(Long userId, Long scheduleId);

    /**
     * Hủy scheduled transaction (đổi status thành CANCELLED, không xóa)
     */
    ScheduledTransactionResponse cancelScheduledTransaction(Long userId, Long scheduleId);

    /**
     * Thực hiện một scheduled transaction (được gọi bởi scheduler)
     */
    void executeScheduledTransaction(ScheduledTransaction scheduledTransaction);

    /**
     * Tính toán ngày thực hiện tiếp theo dựa trên scheduleType
     */
    java.time.LocalDate calculateNextExecutionDate(ScheduledTransaction scheduledTransaction);

    /**
     * Preview ngày thực hiện tiếp theo TRƯỚC KHI tạo scheduled transaction (cho frontend hiển thị mini preview)
     * @param request Request để tạo scheduled transaction
     * @return LocalDate - Ngày thực hiện tiếp theo (hoặc null nếu không hợp lệ)
     */
    java.time.LocalDate previewNextExecutionDate(com.example.financeapp.scheduledtransaction.dto.CreateScheduledTransactionRequest request);

    /**
     * Lấy lịch sử thực hiện của scheduled transaction
     */
    List<ScheduledTransactionLogResponse> getExecutionLogs(Long userId, Long scheduleId);
}

