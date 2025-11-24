package com.example.financeapp.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleLoginRequest {

    @NotBlank(message = "idToken không được để trống")
    private String idToken;
}

