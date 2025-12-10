package com.example.financeapp.wallet.controller;

import com.example.financeapp.wallet.service.ExchangeRateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/exchange")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    public ExchangeRateController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getRate(@RequestParam String from, @RequestParam String to) {
        BigDecimal rate = exchangeRateService.getExchangeRate(from, to);
        Map<String, Object> body = new HashMap<>();
        body.put("from", from.toUpperCase());
        body.put("to", to.toUpperCase());
        body.put("rate", rate);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/convert")
    public ResponseEntity<Map<String, Object>> convert(@RequestParam String from,
                                                       @RequestParam String to,
                                                       @RequestParam(required = false) BigDecimal amount) {
        BigDecimal rate = exchangeRateService.getExchangeRate(from, to);
        Map<String, Object> body = new HashMap<>();
        body.put("from", from.toUpperCase());
        body.put("to", to.toUpperCase());
        body.put("rate", rate);
        if (amount != null) {
            body.put("amount", amount);
            body.put("converted", amount.multiply(rate));
        }
        return ResponseEntity.ok(body);
    }
}
