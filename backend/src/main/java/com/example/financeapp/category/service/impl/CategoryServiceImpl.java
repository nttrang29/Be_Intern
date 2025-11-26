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

        // --- LOGIC MỚI: Cho phép Admin chọn ---
        boolean isAdmin = user.getRole() == com.example.financeapp.security.Role.ADMIN;

        if (isSystem) {
            // Nếu muốn tạo danh mục hệ thống
            if (isAdmin) {
                categoryOwner = null; // System Category không thuộc về user cụ thể
            } else {
                // User thường không được phép tạo System -> Ép về cá nhân
                isSystem = false;
                categoryOwner = user;
            }
        } else {
            // Nếu muốn tạo danh mục cá nhân (cho cả Admin và User thường)
            categoryOwner = user;
        }
        // --------------------------------------

        // Kiểm tra trùng tên (chỉ check các category chưa bị xóa)
        // Sử dụng so sánh chính xác 100% để tránh lỗi do collation của database
        // Logic:
        // - Danh mục hệ thống có thể trùng tên với danh mục cá nhân (chỉ check trùng với danh mục hệ thống khác)
        // - Danh mục cá nhân KHÔNG được trùng tên với danh mục hệ thống (check cả hệ thống và cá nhân)
        boolean duplicate;
        if (isSystem) {
            // Tạo danh mục hệ thống: chỉ kiểm tra trùng với các danh mục hệ thống khác
            List<Category> existingSystem = categoryRepository.findByUserIsNullAndIsSystemTrueAndDeletedFalse();
            duplicate = existingSystem.stream()
                    .anyMatch(c -> c.getCategoryName().equals(name) &&
                            c.getTransactionType().equals(type));
        } else {
            // Tạo danh mục cá nhân: kiểm tra trùng với cả danh mục hệ thống VÀ danh mục cá nhân của user
            // 1. Kiểm tra trùng với danh mục hệ thống
            List<Category> existingSystem = categoryRepository.findByUserIsNullAndIsSystemTrueAndDeletedFalse();
            boolean duplicateWithSystem = existingSystem.stream()
                    .anyMatch(c -> c.getCategoryName().equals(name) &&
                            c.getTransactionType().equals(type));

            // 2. Kiểm tra trùng với danh mục cá nhân của user
            List<Category> existingPersonal = categoryRepository.findByUserAndDeletedFalse(categoryOwner);
            boolean duplicateWithPersonal = existingPersonal.stream()
                    .anyMatch(c -> c.getCategoryName().equals(name) &&
                            c.getTransactionType().equals(type));

            duplicate = duplicateWithSystem || duplicateWithPersonal;
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

        // Kiểm tra nếu đã bị xóa mềm
        if (category.isDeleted()) {
            throw new RuntimeException("Không thể sửa danh mục đã bị xóa");
        }

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

        // Logic cập nhật và check trùng tên khi sửa (chỉ check các category chưa bị xóa, trừ chính nó)
        // Logic tương tự như create:
        // - Danh mục hệ thống có thể trùng tên với danh mục cá nhân (chỉ check trùng với danh mục hệ thống khác)
        // - Danh mục cá nhân KHÔNG được trùng tên với danh mục hệ thống (check cả hệ thống và cá nhân)
        if (name != null && !name.isBlank() && !name.equals(category.getCategoryName())) {
            boolean duplicate;
            if (isSystemCat) {
                // Sửa danh mục hệ thống: chỉ kiểm tra trùng với các danh mục hệ thống khác
                List<Category> existing = categoryRepository.findByUserIsNullAndIsSystemTrueAndDeletedFalse();
                duplicate = existing.stream()
                        .anyMatch(c -> !c.getCategoryId().equals(category.getCategoryId()) &&
                                c.getCategoryName().equals(name) &&
                                c.getTransactionType().equals(category.getTransactionType()));
            } else {
                // Sửa danh mục cá nhân: kiểm tra trùng với cả danh mục hệ thống VÀ danh mục cá nhân khác của user
                // 1. Kiểm tra trùng với danh mục hệ thống
                List<Category> existingSystem = categoryRepository.findByUserIsNullAndIsSystemTrueAndDeletedFalse();
                boolean duplicateWithSystem = existingSystem.stream()
                        .anyMatch(c -> c.getCategoryName().equals(name) &&
                                c.getTransactionType().equals(category.getTransactionType()));

                // 2. Kiểm tra trùng với danh mục cá nhân khác của user (trừ chính nó)
                List<Category> existingPersonal = categoryRepository.findByUserAndDeletedFalse(currentUser);
                boolean duplicateWithPersonal = existingPersonal.stream()
                        .anyMatch(c -> !c.getCategoryId().equals(category.getCategoryId()) &&
                                c.getCategoryName().equals(name) &&
                                c.getTransactionType().equals(category.getTransactionType()));

                duplicate = duplicateWithSystem || duplicateWithPersonal;
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
    // XÓA DANH MỤC (SOFT DELETE)
    // ==============================
    @Override
    public void deleteCategory(User currentUser, Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));

        // Kiểm tra nếu đã bị xóa mềm
        if (category.isDeleted()) {
            throw new RuntimeException("Danh mục này đã bị xóa");
        }

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

        // XÓA MỀM: Chỉ đánh dấu deleted = true, không xóa khỏi database
        category.setDeleted(true);
        categoryRepository.save(category);
    }

    // ==============================
    // LẤY DANH SÁCH DANH MỤC CỦA USER (BỎ QUA CÁC DANH MỤC ĐÃ BỊ XÓA MỀM)
    // ==============================
    @Override
    public List<Category> getCategoriesByUser(User user) {
        // Sử dụng query methods đã filter deleted = false
        List<Category> systemCategories = categoryRepository.findByUserIsNullAndIsSystemTrueAndDeletedFalse();
        List<Category> userCategories = categoryRepository.findByUserAndDeletedFalse(user);

        return Stream.concat(systemCategories.stream(), userCategories.stream())
                .collect(Collectors.toList());
    }
}