-- ============================================
-- MIGRATION: CURRENCY CONVERSION FOR MERGE WALLET
-- Thêm các field mới vào bảng transactions để hỗ trợ chuyển đổi tiền tệ
-- ============================================

-- Thêm các cột mới vào bảng transactions
ALTER TABLE transactions 
ADD COLUMN original_amount DECIMAL(15,2) NULL COMMENT 'Số tiền gốc trước khi chuyển đổi (nếu có)',
ADD COLUMN original_currency VARCHAR(3) NULL COMMENT 'Loại tiền gốc (VD: USD, EUR)',
ADD COLUMN exchange_rate DECIMAL(10,6) NULL COMMENT 'Tỷ giá áp dụng khi chuyển đổi',
ADD COLUMN merge_date TIMESTAMP NULL COMMENT 'Ngày gộp ví (để biết transaction từ merge)';

-- Tạo index để tăng tốc queries
CREATE INDEX idx_transactions_merge_date ON transactions(merge_date);
CREATE INDEX idx_transactions_original_currency ON transactions(original_currency);

-- ============================================
-- GIẢI THÍCH CÁC TRƯỜNG MỚI
-- ============================================

/*
1. original_amount:
   - Lưu số tiền GỐC của transaction trước khi chuyển đổi
   - VD: Transaction ban đầu là $20.00 → sau merge convert sang VND → lưu 20.00
   - NULL nếu transaction không qua chuyển đổi

2. original_currency:
   - Lưu loại tiền GỐC của transaction
   - VD: "USD", "EUR", "JPY"
   - NULL nếu transaction không qua chuyển đổi

3. exchange_rate:
   - Tỷ giá áp dụng khi chuyển đổi
   - VD: 1 USD = 24,350 VND → lưu 24350.000000
   - NULL nếu transaction không qua chuyển đổi

4. merge_date:
   - Ngày thực hiện gộp ví
   - Dùng để phân biệt transaction từ merge vs transaction thường
   - NULL nếu transaction không phải từ merge
*/

-- ============================================
-- VÍ DỤ SỬ DỤNG
-- ============================================

/*
Scenario: Merge "Ví USD" vào "Ví VND"
- Ví USD có transaction: Ăn trưa = $20.00
- Tỷ giá: 1 USD = 24,350 VND
- Sau merge:
  * amount = 487,000 (VND)
  * original_amount = 20.00 (USD)
  * original_currency = 'USD'
  * exchange_rate = 24350.000000
  * merge_date = '2024-11-11 14:30:00'
*/

-- ============================================
-- QUERY MẪU
-- ============================================

-- Lấy tất cả transactions đã qua chuyển đổi
-- SELECT * FROM transactions 
-- WHERE original_currency IS NOT NULL;

-- Lấy transactions từ merge trong tháng này
-- SELECT * FROM transactions 
-- WHERE merge_date >= DATE_SUB(NOW(), INTERVAL 1 MONTH);

-- Tính tổng số tiền gốc theo currency
-- SELECT 
--     original_currency,
--     SUM(original_amount) as total_original,
--     COUNT(*) as transaction_count
-- FROM transactions
-- WHERE original_currency IS NOT NULL
-- GROUP BY original_currency;

-- ============================================
-- VERIFY MIGRATION
-- ============================================

DESCRIBE transactions;

-- ============================================
-- ROLLBACK SCRIPT (nếu cần)
-- ============================================

-- ALTER TABLE transactions 
-- DROP COLUMN original_amount,
-- DROP COLUMN original_currency,
-- DROP COLUMN exchange_rate,
-- DROP COLUMN merge_date;
--
-- DROP INDEX idx_transactions_merge_date ON transactions;
-- DROP INDEX idx_transactions_original_currency ON transactions;

-- ============================================
-- NOTES
-- ============================================
-- 1. Các cột mới đều là NULLABLE → không ảnh hưởng transactions cũ
-- 2. Chỉ transactions từ merge mới có giá trị trong các cột này
-- 3. Transactions thường vẫn hoạt động bình thường (các cột = NULL)
-- 4. Có thể query để phân tích currency conversion history

