package com.example.financeapp;

import com.example.financeapp.security.Role;
import com.example.financeapp.user.entity.User;
import com.example.financeapp.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.TimeZone;

@SpringBootApplication
public class FinanceAppApplication {

    @PostConstruct
    public void init() {
        // Set JVM default timezone to Vietnam (UTC+7)
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }

    public static void main(String[] args) {
        SpringApplication.run(FinanceAppApplication.class, args);
    }

    /**
     * Tạo ADMIN mặc định khi chạy lần đầu
     */
    @Bean
    CommandLineRunner initAdmin(UserRepository userRepository, PasswordEncoder encoder) {
        return args -> {
            String adminEmail = "admin@financeapp.com";

            if (!userRepository.existsByEmail(adminEmail)) {
                User admin = new User();
                admin.setEmail(adminEmail);
                admin.setFullName("System Administrator");
                admin.setPasswordHash(encoder.encode("Admin@123"));  // mật khẩu mặc định
                admin.setRole(Role.ADMIN);
                admin.setLocked(false);
                admin.setGoogleAccount(false);
                admin.setFirstLogin(false);
                admin.setEnabled(true);
                admin.setDeleted(false);
                admin.setProvider("local");

                userRepository.save(admin);

                System.out.println("====> ADMIN CREATED: " + adminEmail + " / Admin@123");
            } else {
                System.out.println("====> ADMIN ALREADY EXISTS, SKIP");
            }
        };
    }
}
