package com.example.financeapp.controller;

import com.example.financeapp.dto.CreateCategoryRequest;
import com.example.financeapp.entity.Category;
import com.example.financeapp.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    @Autowired private CategoryService categoryService;

    // Tạo danh mục
    @PostMapping("/create")
    public Category createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return categoryService.createCategory(
                request.getUserId(),
                request.getCategoryName(),
                request.getIcon(),
                request.getTransactionTypeId()
        );
    }
}
