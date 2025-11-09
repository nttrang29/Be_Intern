package com.example.financeapp.config;

import com.example.financeapp.entity.Currency;
import com.example.financeapp.repository.CurrencyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Lớp này sẽ tự động chạy khi ứng dụng khởi động.
 * Nó dùng để "gieo" dữ liệu mầm (seed data) cho database.
 */
@Component // Báo cho Spring biết đây là một Bean và cần được quản lý
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private CurrencyRepository currencyRepository;

    @Override
    public void run(String... args) throws Exception {
        // Kiểm tra xem bảng 'currencies' đã có dữ liệu hay chưa
        if (currencyRepository.count() == 0) {
            // Nếu bảng trống, tiến hành thêm dữ liệu
            System.out.println(">>> [DataSeeder] Bảng 'currencies' trống. Bắt đầu thêm dữ liệu mẫu...");

            // Tạo tiền tệ VND
            Currency vnd = new Currency();
            vnd.setCurrencyCode("VND");
            vnd.setCurrencyName("Vietnamese Dong");
            vnd.setSymbol("₫");

            // Tạo tiền tệ USD
            Currency usd = new Currency();
            usd.setCurrencyCode("USD");
            usd.setCurrencyName("US Dollar");
            usd.setSymbol("$");

            // Lưu cả hai vào database
            currencyRepository.saveAll(List.of(vnd, usd));

            System.out.println(">>> [DataSeeder] Đã thêm thành công 2 loại tiền tệ (VND, USD).");

        } else {
            // Nếu bảng đã có dữ liệu, thì bỏ qua
            System.out.println(">>> [DataSeeder] Bảng 'currencies' đã có dữ liệu. Bỏ qua seeding.");
        }
    }
}