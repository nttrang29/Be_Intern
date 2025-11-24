package com.example.financeapp.auth.dto;

import jakarta.validation.constraints.*;

import lombok.Data;

@Data
public class RegisterRequest {

    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Email không được để trống")
    private String email;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ tên phải từ 2 - 100 ký tự")
    private String fullName;
}
