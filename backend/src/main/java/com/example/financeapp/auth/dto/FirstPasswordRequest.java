package com.example.financeapp.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FirstPasswordRequest {

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{}|;:'\",.<>/?~]).{8,}$",
            message = "Mật khẩu mới phải có chữ hoa, chữ thường, số và ký tự đặc biệt, tối thiểu 8 ký tự"
    )
    private String newPassword;
}
