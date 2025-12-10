package com.example.financeapp.wallet.service;

import com.example.financeapp.wallet.service.impl.ExchangeRateServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExchangeRateServiceImplTest {

    @Test
    public void fallbackRateAndConversion_whenApiUnavailable() {
        WebClient.Builder builder = WebClient.builder();

        // Use an invalid base URL so external API call fails fast
        String badBase = "http://127.0.0.1:9/convert"; // closed port
        String badGooglePattern = "http://127.0.0.1:9/%s-%s";

        ExchangeRateServiceImpl svc = new ExchangeRateServiceImpl(builder, badBase, 300, badGooglePattern);

        // Fallback logic should compute rate from internal FALLBACK_RATES
        BigDecimal rate = svc.getExchangeRate("USD", "VND");

        BigDecimal expectedRate = BigDecimal.ONE.divide(new BigDecimal("0.000041"), 12, RoundingMode.HALF_UP);
        assertEquals(0, rate.compareTo(expectedRate), "Fallback USD->VND rate should match expected");

        BigDecimal amount = new BigDecimal("100");
        BigDecimal converted = svc.convertAmount(amount, "USD", "VND");
        BigDecimal expectedConverted = amount.multiply(expectedRate).setScale(8, RoundingMode.HALF_UP);
        assertEquals(0, converted.compareTo(expectedConverted), "Converted amount should match expected using fallback rate");
    }
}
