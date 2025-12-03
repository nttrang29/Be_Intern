package com.example.financeapp.fund.service;

import com.example.financeapp.fund.dto.CreateFundRequest;
import com.example.financeapp.fund.dto.FundResponse;
import com.example.financeapp.fund.dto.UpdateFundRequest;

import java.util.List;

/**
 * Service để quản lý quỹ tiết kiệm
 */
public interface FundService {

    /**
     * Tạo quỹ mới
     */
    FundResponse createFund(Long userId, CreateFundRequest request);

    /**
     * Lấy tất cả quỹ của user (cả cá nhân và nhóm tham gia)
     */
    List<FundResponse> getAllFunds(Long userId);

    /**
     * Lấy quỹ cá nhân của user
     */
    List<FundResponse> getPersonalFunds(Long userId, Boolean hasDeadline);

    /**
     * Lấy quỹ nhóm của user (chủ quỹ hoặc thành viên)
     */
    List<FundResponse> getGroupFunds(Long userId, Boolean hasDeadline);

    /**
     * Lấy quỹ nhóm mà user tham gia (không phải chủ quỹ)
     */
    List<FundResponse> getParticipatedFunds(Long userId);

    /**
     * Lấy chi tiết một quỹ
     */
    FundResponse getFundById(Long userId, Long fundId);

    /**
     * Cập nhật quỹ
     */
    FundResponse updateFund(Long userId, Long fundId, UpdateFundRequest request);

    /**
     * Đóng quỹ (tạm dừng)
     */
    void closeFund(Long userId, Long fundId);

    /**
     * Xóa quỹ
     */
    void deleteFund(Long userId, Long fundId);

    /**
     * Nạp tiền vào quỹ
     */
    FundResponse depositToFund(Long userId, Long fundId, java.math.BigDecimal amount);

    /**
     * Rút tiền từ quỹ (chỉ cho quỹ không kỳ hạn)
     */
    FundResponse withdrawFromFund(Long userId, Long fundId, java.math.BigDecimal amount);

    /**
     * Kiểm tra ví có đang được sử dụng cho quỹ hoặc ngân sách không
     */
    boolean isWalletUsed(Long walletId);
}

