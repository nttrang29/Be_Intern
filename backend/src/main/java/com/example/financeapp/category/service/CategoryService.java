package com.example.financeapp.service;

import com.example.financeapp.entity.Category;
import com.example.financeapp.entity.User;

import java.util.List;

public interface CategoryService {
    Category createCategory(User user, String name, String description, Long transactionTypeId);
    Category updateCategory(User currentUser, Long id, String name, String description);
    void deleteCategory(User currentUser, Long id);
    List<Category> getCategoriesByUser(User user);
}