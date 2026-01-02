package com.example.financeapp.category.repository;

import com.example.financeapp.category.entity.Category;
import com.example.financeapp.transaction.entity.TransactionType;
import com.example.financeapp.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUser(User user);
    List<Category> findByUserAndTransactionType(User user, TransactionType type);
    List<Category> findByUser_UserId(Long userId);
    List<Category> findByUserIsNullAndIsSystemTrue();

    // Query methods với filter deleted = false
    List<Category> findByUserAndDeletedFalse(User user);
    List<Category> findByUserIsNullAndIsSystemTrueAndDeletedFalse();

    boolean existsByCategoryNameAndTransactionTypeAndUser(
            String categoryName, TransactionType transactionType, User user
    );

    boolean existsByCategoryNameAndTransactionTypeAndUserIsNullAndIsSystemTrue(
            String categoryName, TransactionType transactionType
    );

    // Check trùng tên (chỉ check các category chưa bị xóa)
    boolean existsByCategoryNameAndTransactionTypeAndUserAndDeletedFalse(
            String categoryName, TransactionType transactionType, User user
    );

    boolean existsByCategoryNameAndTransactionTypeAndUserIsNullAndIsSystemTrueAndDeletedFalse(
            String categoryName, TransactionType transactionType
    );
}