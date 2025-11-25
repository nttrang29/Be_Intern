package com.example.financeapp.category.service.impl;

import com.example.financeapp.category.entity.Category;
import com.example.financeapp.transaction.entity.TransactionType;
import com.example.financeapp.user.entity.User;
import com.example.financeapp.category.repository.CategoryRepository;
import com.example.financeapp.transaction.repository.TransactionRepository;
import com.example.financeapp.transaction.repository.TransactionTypeRepository;
import com.example.financeapp.category.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionTypeRepository transactionTypeRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    // ==============================
    // TẠO DANH MỤC
    // ==============================
    @Override
    public Category createCategory(User user, String name, String description, Long transactionTypeId, boolean isSystem) {
        TransactionType type = transactionTypeRepository.findById(transactionTypeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại giao dịch"));

        User categoryOwner;

        // --- LOGIC QUAN TRỌNG: FIX CỨNG THEO ROLE ---
        if (user.getRole() == com.example.financeapp.security.Role.ADMIN) {
            isSystem = true;       // Admin -> Mặc định là hệ thống
            categoryOwner = null;  // Hệ thống -> Không thuộc về user cụ thể (để hiện cho tất cả)
        } else {
            isSystem = false;      // User thường -> Mặc định là cá nhân
            categoryOwner = user;  // Gán cho user đó
        }
        // ---------------------------------------------

        // Kiểm tra trùng tên
        boolean duplicate;
        if (isSystem) {
            // Check trong danh sách hệ thống
            duplicate = categoryRepository.existsByCategoryNameAndTransactionTypeAndUserIsNullAndIsSystemTrue(name, type);
        } else {
            // Check trong danh sách cá nhân
            duplicate = categoryRepository.existsByCategoryNameAndTransactionTypeAndUser(name, type, categoryOwner);
        }

        if (duplicate) {
            throw new RuntimeException("Danh mục '" + name + "' đã tồn tại");
        }

        Category category = new Category(name, type, description, categoryOwner, isSystem);
        return categoryRepository.save(category);
    }

    @Override
    public Category updateCategory(User currentUser, Long id, String name, String description) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));

        boolean isAdmin = currentUser.getRole() == com.example.financeapp.security.Role.ADMIN;
        boolean isSystemCat = category.isSystem();
        boolean isOwner = category.getUser() != null && category.getUser().getUserId().equals(currentUser.getUserId());

        // Logic phân quyền sửa
        if (isSystemCat) {
            if (!isAdmin) {
                throw new RuntimeException("Chỉ Admin mới được sửa danh mục hệ thống");
            }
        } else {
            if (!isOwner) {
                throw new RuntimeException("Bạn không có quyền sửa danh mục này");
            }
        }

        // Logic cập nhật và check trùng tên khi sửa
        if (name != null && !name.isBlank() && !name.equals(category.getCategoryName())) {
            boolean duplicate;
            if (isSystemCat) {
                duplicate = categoryRepository.existsByCategoryNameAndTransactionTypeAndUserIsNullAndIsSystemTrue(name, category.getTransactionType());
            } else {
                duplicate = categoryRepository.existsByCategoryNameAndTransactionTypeAndUser(name, category.getTransactionType(), currentUser);
            }
            if (duplicate) throw new RuntimeException("Tên danh mục đã tồn tại");
            category.setCategoryName(name);
        }

        if (description != null && !description.isBlank()) {
            category.setDescription(description);
        } else if (description != null && description.isBlank()) {
            category.setDescription(null);
        }

        return categoryRepository.save(category);
    }
    // ==============================
    // XÓA DANH MỤC
    // ==============================
    @Override
    public void deleteCategory(User currentUser, Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));

        boolean isAdmin = currentUser.getRole() == com.example.financeapp.security.Role.ADMIN;
        boolean isSystemCat = category.isSystem();
        boolean isOwner = category.getUser() != null && category.getUser().getUserId().equals(currentUser.getUserId());

        // Logic phân quyền xóa
        if (isSystemCat) {
            if (!isAdmin) {
                throw new RuntimeException("Chỉ Admin mới được xóa danh mục hệ thống");
            }
        } else {
            if (!isOwner) {
                throw new RuntimeException("Bạn không có quyền xóa danh mục này");
            }
        }

        boolean hasTransactions = transactionRepository.existsByCategory_CategoryId(id);
        if (hasTransactions) {
            throw new RuntimeException("Danh mục đã có giao dịch, không thể xóa");
        }

        categoryRepository.delete(category);
    }

    // ==============================
    // LẤY DANH SÁCH DANH MỤC CỦA USER
    // ==============================
    @Override
    public List<Category> getCategoriesByUser(User user) {
        List<Category> systemCategories = categoryRepository.findByUserIsNullAndIsSystemTrue();
        List<Category> userCategories = categoryRepository.findByUser(user);

        return Stream.concat(systemCategories.stream(), userCategories.stream())
                .collect(Collectors.toList());
    }
}