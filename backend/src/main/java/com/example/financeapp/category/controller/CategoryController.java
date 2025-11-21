package com.example.financeapp.controller;

import com.example.financeapp.dto.CreateCategoryRequest;
import com.example.financeapp.dto.UpdateCategoryRequest;
import com.example.financeapp.entity.Category;
import com.example.financeapp.entity.User;
import com.example.financeapp.security.CustomUserDetails;
import com.example.financeapp.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    @Autowired private CategoryService categoryService;

    @PostMapping("/create")
    public Category createCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        User user = getUserFromDetails(userDetails);
        return categoryService.createCategory(
                user,
                request.getCategoryName(),
                request.getDescription(),
                request.getTransactionTypeId()
        );
    }

    @PutMapping("/{id}")
    public Category updateCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        User user = userDetails.getUser();
        return categoryService.updateCategory(
                user,
                id,
                request.getCategoryName(),
                request.getDescription()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id
    ) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = getUserFromDetails(userDetails);
            categoryService.deleteCategory(user, id);
            res.put("message", "Danh mục đã được xóa thành công");
            return ResponseEntity.ok(res);
        } catch (RuntimeException e) {
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("error", "Lỗi máy chủ nội bộ: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    @GetMapping
    public List<Category> getUserCategories(@AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getUserFromDetails(userDetails);
        return categoryService.getCategoriesByUser(user);
    }

    // Helper method để tránh lặp code
    private User getUserFromDetails(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getUser() == null) {
            throw new RuntimeException("Không tìm thấy thông tin người dùng");
        }
        return userDetails.getUser();
    }
}
