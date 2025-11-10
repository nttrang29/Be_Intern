package com.example.financeapp.repository;

import com.example.financeapp.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByTransactionType_TypeId(Long typeId);
}