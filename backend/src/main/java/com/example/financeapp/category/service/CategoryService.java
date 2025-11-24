package com.example.financeapp.category.service;

import com.example.financeapp.category.entity.Category;
import com.example.financeapp.user.entity.User;

import java.util.List;

public interface CategoryService {
    Category createCategory(User user, String name, String description, Long transactionTypeId, boolean isSystem);
    Category updateCategory(User currentUser, Long id, String name, String description);
    void deleteCategory(User currentUser, Long id);
    List<Category> getCategoriesByUser(User user);
}