package com.example.financeapp.transaction.repository;

import com.example.financeapp.transaction.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TransactionTypeRepository extends JpaRepository<TransactionType, Long> {
    Optional<TransactionType> findByTypeName(String typeName);
}