
package com.example.financeapp.repository;

import com.example.financeapp.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CurrencyRepository extends JpaRepository<Currency, String> {
    boolean existsById(String currencyCode);

    Optional<Object> findByCurrencyCode(String currencyCode);
}