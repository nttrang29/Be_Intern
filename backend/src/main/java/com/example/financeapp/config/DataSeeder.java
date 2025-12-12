package com.example.financeapp.config;

import com.example.financeapp.category.entity.Category;
import com.example.financeapp.category.repository.CategoryRepository;
import com.example.financeapp.transaction.entity.TransactionType;
import com.example.financeapp.transaction.repository.TransactionTypeRepository;
import com.example.financeapp.wallet.entity.Currency;
import com.example.financeapp.wallet.repository.CurrencyRepository;
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

            currencyRepository.save(vnd);
            System.out.println(">>> [DataSeeder] Đã thêm thành công loại tiền tệ VND.");
        } else {
            System.out.println(">>> [DataSeeder] Bảng 'currencies' đã có dữ liệu. Bỏ qua seeding.");
        }
    }

    private void seedTransactionTypesAndCategories() {
        if (transactionTypeRepository.count() == 0) {
            System.out.println(">>> [DataSeeder] Bảng 'transaction_types' trống. Bắt đầu thêm dữ liệu mẫu...");

            TransactionType expense = new TransactionType();
            expense.setTypeName("Chi tiêu");
            transactionTypeRepository.save(expense);

            TransactionType income = new TransactionType();
            income.setTypeName("Thu nhập");
            transactionTypeRepository.save(income);

            // 2. Danh mục Chi tiêu (expense)
            createCategory("Ăn uống",          expense, "Ăn uống hàng ngày, nhà hàng, cà phê, đồ ăn vặt");
            createCategory("Di chuyển",        expense, "Xăng xe, gửi xe, Grab, taxi, vé xe bus, tàu");
            createCategory("Mua sắm",          expense, "Quần áo, giày dép, phụ kiện, đồ dùng cá nhân");
            createCategory("Giải trí",         expense, "Xem phim, karaoke, du lịch, chơi game, sự kiện");
            createCategory("Hóa đơn",          expense, "Điện, nước, internet, điện thoại, truyền hình");
            createCategory("Sức khỏe",         expense, "Khám bệnh, thuốc men, gym, yoga, thể thao");
            createCategory("Giáo dục",         expense, "Học phí, sách vở, khóa học, dụng cụ học tập");
            createCategory("Khác",             expense, "Các khoản chi tiêu khác không thuộc nhóm trên");

            // 3. Danh mục Thu nhập (income)
            createCategory("Lương",            income, "Lương tháng, lương thưởng từ công việc chính");
            createCategory("Thưởng",           income, "Thưởng hiệu suất, thưởng lễ Tết, thưởng dự án");
            createCategory("Đầu tư",           income, "Lãi cổ phiếu, lãi tiết kiệm, lợi nhuận kinh doanh");
            createCategory("Quà tặng",         income, "Tiền mừng cưới, sinh nhật, quà biếu từ người thân");
            createCategory("Khác",             income, "Các khoản thu nhập khác không thuộc nhóm trên");

            System.out.println(">>> [DataSeeder] Đã thêm TransactionType + Category mẫu thành công.");
        } else {
            System.out.println(">>> [DataSeeder] Bảng 'transaction_types' đã có dữ liệu. Bỏ qua seeding.");
        }
    }

    private void createCategory(String name, TransactionType type, String description) {
        Category category = new Category();
        category.setCategoryName(name);
        category.setTransactionType(type);
        category.setDescription(description);
        category.setUser(null);
        category.setSystem(true);
        categoryRepository.save(category);
    }
}