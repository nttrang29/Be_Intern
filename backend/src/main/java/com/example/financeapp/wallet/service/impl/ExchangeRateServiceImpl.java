package com.example.financeapp.wallet.service.impl;

import com.example.financeapp.wallet.service.ExchangeRateService;
import com.fasterxml.jackson.databind.JsonNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ExchangeRateServiceImpl implements ExchangeRateService {

    // Fallback rates (kept for resilience)
    private static final Map<String, BigDecimal> FALLBACK_RATES = new HashMap<>();

    static {
        FALLBACK_RATES.put("VND", BigDecimal.ONE);
        FALLBACK_RATES.put("USD", new BigDecimal("0.000041"));
        FALLBACK_RATES.put("EUR", new BigDecimal("0.000038"));
        FALLBACK_RATES.put("JPY", new BigDecimal("0.0063"));
        FALLBACK_RATES.put("GBP", new BigDecimal("0.000032"));
        FALLBACK_RATES.put("CNY", new BigDecimal("0.00030"));
    }

    private final WebClient webClient;
    private final String baseUrl;
    private final long cacheTtlMillis;
    private final String googleFinanceUrlPattern;

    private static final class CachedRate {
        final BigDecimal rate;
        final long ts;

        CachedRate(BigDecimal rate, long ts) {
            this.rate = rate;
            this.ts = ts;
        }
    }

    private final ConcurrentHashMap<String, CachedRate> cache = new ConcurrentHashMap<>();

    public ExchangeRateServiceImpl(WebClient.Builder webClientBuilder,
                                   @Value("${app.exchange.base-url:https://api.exchangerate.host/convert}") String baseUrl,
                                   @Value("${app.exchange.cache-ttl-sec:300}") long cacheTtlSec,
                                   @Value("${app.exchange.google-finance-url-pattern:https://www.google.com/finance/quote/%s-%s}") String googleFinanceUrlPattern) {
        this.webClient = webClientBuilder.build();
        this.baseUrl = baseUrl;
        this.cacheTtlMillis = cacheTtlSec * 1000L;
        this.googleFinanceUrlPattern = googleFinanceUrlPattern;
    }

    @Override
    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        if (fromCurrency == null || toCurrency == null) {
            throw new RuntimeException("Loại tiền tệ không được null");
        }

        String from = fromCurrency.toUpperCase();
        String to = toCurrency.toUpperCase();

        if (from.equals(to)) {
            return BigDecimal.ONE;
        }

        String key = from + ":" + to;
        CachedRate cached = cache.get(key);
        long now = System.currentTimeMillis();
        if (cached != null && (now - cached.ts) <= cacheTtlMillis) {
            return cached.rate;
        }

        // Special-case: for USD <-> VND prefer Google Finance (user requested)
        if ((from.equals("USD") && to.equals("VND")) || (from.equals("VND") && to.equals("USD"))) {
            try {
                BigDecimal googleRate = fetchFromGoogleFinance(from, to);
                if (googleRate != null) {
                    cache.put(key, new CachedRate(googleRate, now));
                    return googleRate;
                }
            } catch (Exception ignored) {
            }
        }

        // Try fetch from external API (exchangerate.host) as primary official source
        try {
            String uri = String.format("%s?from=%s&to=%s", baseUrl, from, to);
            JsonNode root = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(5));

            if (root != null) {
                JsonNode info = root.path("info");
                if (info != null && info.has("rate")) {
                    BigDecimal rate = new BigDecimal(info.path("rate").asText());
                    cache.put(key, new CachedRate(rate, now));
                    return rate;
                }
                if (root.has("result")) {
                    BigDecimal rate = new BigDecimal(root.path("result").asText());
                    cache.put(key, new CachedRate(rate, now));
                    return rate;
                }
            }
        } catch (Exception e) {
            // ignore and try fallback sources
        }

        // If API fails, try Google Finance HTML as a last-resort fallback
        try {
            BigDecimal googleRate = fetchFromGoogleFinance(from, to);
            if (googleRate != null) {
                cache.put(key, new CachedRate(googleRate, now));
                return googleRate;
            }
        } catch (Exception ignored) {
        }

        // Fallback to internal rates (compute via VND base)
        if (!FALLBACK_RATES.containsKey(from)) {
            throw new RuntimeException("Loại tiền tệ không được hỗ trợ: " + from);
        }
        if (!FALLBACK_RATES.containsKey(to)) {
            throw new RuntimeException("Loại tiền tệ không được hỗ trợ: " + to);
        }

        BigDecimal fromRate = FALLBACK_RATES.get(from);
        BigDecimal toRate = FALLBACK_RATES.get(to);

        BigDecimal computed;
        if (from.equals("VND")) {
            computed = toRate;
        } else if (to.equals("VND")) {
            computed = BigDecimal.ONE.divide(fromRate, 12, RoundingMode.HALF_UP);
        } else {
            computed = toRate.divide(fromRate, 12, RoundingMode.HALF_UP);
        }

        cache.put(key, new CachedRate(computed, now));
        return computed;
    }

    private BigDecimal fetchFromGoogleFinance(String from, String to) {
        String url = String.format(googleFinanceUrlPattern, from, to);
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(5000)
                    .get();

            // Common selector used in Google Finance for price display
            Elements els = doc.select("div.YMlKec.fxKbKc, div[data-last-price], span[class*=YMlKec]");
            for (Element el : els) {
                String txt = el.text().replaceAll("[^0-9.,]", "");
                txt = txt.replaceAll(",", "");
                if (!txt.isBlank()) {
                    return new BigDecimal(txt);
                }
            }

            // Fallback to scanning page text for a likely numeric value (first large number)
            Pattern p = Pattern.compile("([0-9]{1,3}(?:[,\\.][0-9]+)+)");
            Matcher m = p.matcher(doc.text());
            if (m.find()) {
                String found = m.group(1).replaceAll(",", "");
                return new BigDecimal(found);
            }
        } catch (Exception e) {
            // any exception -> return null to trigger next fallback
        }
        return null;
    }

    @Override
    public BigDecimal convertAmount(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Số tiền không hợp lệ");
        }

        BigDecimal rate = getExchangeRate(fromCurrency, toCurrency);
        return amount.multiply(rate).setScale(8, RoundingMode.HALF_UP);
    }
}

