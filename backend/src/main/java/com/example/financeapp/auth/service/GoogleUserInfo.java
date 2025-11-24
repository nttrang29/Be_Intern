package com.example.financeapp.auth.service;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GoogleUserInfo {
    private String email;
    private String name;
    private String picture;
}
