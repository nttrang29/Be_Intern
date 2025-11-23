package com.example.financeapp.admin.dto;

import jakarta.validation.constraints.NotBlank;

public class ChangeRoleRequest {

    @NotBlank(message = "Role không được để trống")
    private String role; // "USER" hoặc "ADMIN"

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}

