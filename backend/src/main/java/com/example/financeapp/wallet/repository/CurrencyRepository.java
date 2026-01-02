
package com.example.financeapp.wallet.repository;

import com.example.financeapp.wallet.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CurrencyRepository extends JpaRepository<Currency, String> {
    boolean existsById(String currencyCode);

    Optional<Object> findByCurrencyCode(String currencyCode);
}