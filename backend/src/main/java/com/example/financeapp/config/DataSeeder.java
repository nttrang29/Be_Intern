package com.example.financeapp.config;

import com.example.financeapp.entity.*;
import com.example.financeapp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Lớp này sẽ tự động chạy khi ứng dụng khởi động.
 * Nó dùng để "gieo" dữ liệu mầm (seed data) cho database.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired private CurrencyRepository currencyRepository;
    @Autowired private TransactionTypeRepository transactionTypeRepository;
    @Autowired private CategoryRepository categoryRepository;

    @Override
    public void run(String... args) throws Exception {
        seedCurrencies();
        seedTransactionTypesAndCategories();
    }

    private void seedCurrencies() {
        if (currencyRepository.count() == 0) {
            System.out.println(">>> [DataSeeder] Bảng 'currencies' trống. Bắt đầu thêm dữ liệu mẫu...");

            Currency vnd = new Currency();
            vnd.setCurrencyCode("VND");
            vnd.setCurrencyName("Vietnamese Dong");
            vnd.setSymbol("₫");

            Currency usd = new Currency();
            usd.setCurrencyCode("USD");
            usd.setCurrencyName("US Dollar");
            usd.setSymbol("$");

            currencyRepository.saveAll(List.of(vnd, usd));
            System.out.println(">>> [DataSeeder] Đã thêm thành công 2 loại tiền tệ (VND, USD).");
        } else {
            System.out.println(">>> [DataSeeder] Bảng 'currencies' đã có dữ liệu. Bỏ qua seeding.");
        }
    }

    private void seedTransactionTypesAndCategories() {
        if (transactionTypeRepository.count() == 0) {
            System.out.println(">>> [DataSeeder] Bảng 'transaction_types' trống. Bắt đầu thêm dữ liệu mẫu...");

            // 1. Tạo loại giao dịch
            TransactionType expense = new TransactionType();
            expense.setTypeName("Chi tiêu");
            transactionTypeRepository.save(expense);

            TransactionType income = new TransactionType();
            income.setTypeName("Thu nhập");
            transactionTypeRepository.save(income);

            // 2. Tạo danh mục cho Chi tiêu
            createCategory("Ăn uống", expense, "food");
            createCategory("Di chuyển", expense, "transport");
            createCategory("Mua sắm", expense, "shopping");
            createCategory("Giải trí", expense, "entertainment");
            createCategory("Hóa đơn", expense, "bills");
            createCategory("Sức khỏe", expense, "health");
            createCategory("Giáo dục", expense, "education");
            createCategory("Khác", expense, "other");

            // 3. Tạo danh mục cho Thu nhập
            createCategory("Lương", income, "salary");
            createCategory("Thưởng", income, "bonus");
            createCategory("Đầu tư", income, "investment");
            createCategory("Quà tặng", income, "gift");
            createCategory("Khác", income, "other");

            System.out.println(">>> [DataSeeder] Đã thêm TransactionType + Category mẫu thành công.");
        } else {
            System.out.println(">>> [DataSeeder] Bảng 'transaction_types' đã có dữ liệu. Bỏ qua seeding.");
        }
    }

    // Hàm hỗ trợ tạo danh mục
    private void createCategory(String name, TransactionType type, String icon) {
        Category category = new Category();
        category.setCategoryName(name);
        category.setTransactionType(type);
        category.setIcon(icon);
        categoryRepository.save(category);
    }
}