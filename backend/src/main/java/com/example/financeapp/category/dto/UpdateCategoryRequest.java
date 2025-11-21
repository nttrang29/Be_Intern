package com.example.financeapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCategoryRequest {
    @NotBlank(message = "Tên danh mục không được để trống")
    private String categoryName;

    private String description;
}