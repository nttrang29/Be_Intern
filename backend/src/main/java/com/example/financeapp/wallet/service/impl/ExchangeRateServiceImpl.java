package com.example.financeapp.wallet.service.impl;

import com.example.financeapp.wallet.service.ExchangeRateService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation của ExchangeRateService
 *
 * Version 1.0: Sử dụng tỷ giá cố định (hardcoded)
 * TODO: Tích hợp API lấy tỷ giá real-time (VD: exchangerate-api.com, fixer.io)
 */
@Service
public class ExchangeRateServiceImpl implements ExchangeRateService {

    // Tỷ giá cố định (base currency: VND)
    // Trong production, nên lấy từ API hoặc database
    private static final Map<String, BigDecimal> EXCHANGE_RATES = new HashMap<>();

    static {
        // Tỷ giá tham khảo (1 VND = ?)
        EXCHANGE_RATES.put("VND", BigDecimal.ONE); // 1 VND = 1 VND
        EXCHANGE_RATES.put("USD", new BigDecimal("0.000041")); // 1 VND = 0.000041 USD
        EXCHANGE_RATES.put("EUR", new BigDecimal("0.000038")); // 1 VND = 0.000038 EUR
        EXCHANGE_RATES.put("JPY", new BigDecimal("0.0063")); // 1 VND = 0.0063 JPY
        EXCHANGE_RATES.put("GBP", new BigDecimal("0.000032")); // 1 VND = 0.000032 GBP
        EXCHANGE_RATES.put("CNY", new BigDecimal("0.00030")); // 1 VND = 0.0003 CNY

        // Hoặc tính theo chiều ngược (1 Currency = ? VND)
        // USD: 1 USD = 24,350 VND
        // EUR: 1 EUR = 26,315 VND
        // JPY: 1 JPY = 158 VND
        // GBP: 1 GBP = 31,250 VND
        // CNY: 1 CNY = 3,333 VND
    }

    @Override
    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        // Validate input
        if (fromCurrency == null || toCurrency == null) {
            throw new RuntimeException("Loại tiền tệ không được null");
        }

        fromCurrency = fromCurrency.toUpperCase();
        toCurrency = toCurrency.toUpperCase();

        // Nếu cùng currency → tỷ giá = 1
        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE;
        }

        // Kiểm tra currency có được hỗ trợ không
        if (!EXCHANGE_RATES.containsKey(fromCurrency)) {
            throw new RuntimeException("Loại tiền tệ không được hỗ trợ: " + fromCurrency);
        }

        if (!EXCHANGE_RATES.containsKey(toCurrency)) {
            throw new RuntimeException("Loại tiền tệ không được hỗ trợ: " + toCurrency);
        }

        // Tính tỷ giá: from → VND → to
        // VD: USD → VND: 1 USD = ? VND
        //     1 / rateUSD = 1 / 0.000041 = 24,390.243902439024...
        // Tối ưu: Tính trực tiếp từ EXCHANGE_RATES để tránh sai số tích lũy
        // Tỷ giá from → to = EXCHANGE_RATES[to] / EXCHANGE_RATES[from]
        // VD: USD → EUR = 0.000038 / 0.000041 = 0.926829268292683

        // Trường hợp đặc biệt: Nếu from là VND, tỷ giá = EXCHANGE_RATES[to]
        if (fromCurrency.equals("VND")) {
            return EXCHANGE_RATES.get(toCurrency);
        }

        // Trường hợp đặc biệt: Nếu to là VND, tỷ giá = 1 / EXCHANGE_RATES[from]
        if (toCurrency.equals("VND")) {
            return BigDecimal.ONE.divide(EXCHANGE_RATES.get(fromCurrency), 12, RoundingMode.HALF_UP);
        }

        // Trường hợp chung: from → VND → to
        // Tỷ giá = EXCHANGE_RATES[to] / EXCHANGE_RATES[from]
        // Tính trực tiếp để tránh sai số tích lũy từ phép chia 1 / rate
        BigDecimal fromRate = EXCHANGE_RATES.get(fromCurrency);
        BigDecimal toRate = EXCHANGE_RATES.get(toCurrency);
        return toRate.divide(fromRate, 12, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal convertAmount(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Số tiền không hợp lệ");
        }

        BigDecimal rate = getExchangeRate(fromCurrency, toCurrency);

        // Convert: amount * rate
        // VD: $20 * 24,350 = 487,000 VND
        // Sử dụng scale = 8 để giữ độ chính xác cao, tránh mất số tiền nhỏ khi chuyển đổi
        // Ví dụ: 1 VND = 0.000041 USD, nếu làm tròn về 2 chữ số sẽ thành 0.00 USD
        return amount.multiply(rate).setScale(8, RoundingMode.HALF_UP);
    }
}

