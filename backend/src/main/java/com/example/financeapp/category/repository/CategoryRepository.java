package com.example.financeapp.repository;

import com.example.financeapp.entity.Category;
import com.example.financeapp.entity.TransactionType;
import com.example.financeapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUser(User user);
    List<Category> findByUserAndTransactionType(User user, TransactionType type);
    List<Category> findByUser_UserId(Long userId);
    List<Category> findByUserIsNullAndIsSystemTrue();

    boolean existsByCategoryNameAndTransactionTypeAndUser(
            String categoryName, TransactionType transactionType, User user
    );

    boolean existsByCategoryNameAndTransactionTypeAndUserIsNullAndIsSystemTrue(
            String categoryName, TransactionType transactionType
    );
}