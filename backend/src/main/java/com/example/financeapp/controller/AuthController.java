package com.example.financeapp.controller;

import org.springframework.web.bind.annotation.*;

// TODO: sẽ nhận /auth/login, /auth/register
@RestController
@RequestMapping("/auth")
public class AuthController {

    @PostMapping("/login")
    public String login() {
        return "login placeholder";
    }

    @PostMapping("/register")
    public String register() {
        return "register placeholder";
    }
}
