package com.example.financeapp.wallet.service;

import java.math.BigDecimal;

/**
 * Service để lấy tỷ giá hối đoái giữa các loại tiền tệ
 */
public interface ExchangeRateService {

    /**
     * Lấy tỷ giá chuyển đổi từ fromCurrency sang toCurrency
     *
     * @param fromCurrency Loại tiền nguồn (VD: "USD")
     * @param toCurrency Loại tiền đích (VD: "VND")
     * @return Tỷ giá (VD: 1 USD = 24,350 VND → return 24350.0)
     */
    BigDecimal getExchangeRate(String fromCurrency, String toCurrency);

    /**
     * Chuyển đổi số tiền từ currency này sang currency khác
     *
     * @param amount Số tiền cần chuyển
     * @param fromCurrency Loại tiền nguồn
     * @param toCurrency Loại tiền đích
     * @return Số tiền sau khi chuyển đổi
     */
    BigDecimal convertAmount(BigDecimal amount, String fromCurrency, String toCurrency);
}

