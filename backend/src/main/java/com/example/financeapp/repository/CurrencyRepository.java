
package com.example.financeapp.repository;

import com.example.financeapp.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyRepository extends JpaRepository<Currency, String> {
    boolean existsById(String currencyCode);
}