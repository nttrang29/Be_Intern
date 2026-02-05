# Backend - Ứng dụng Quản Lý Tài Chính Cá Nhân
## Giới thiệu
Ứng dụng quản lý tài chính cá nhân giúp người dùng theo dõi và kiểm soát chi tiêu một cách thông minh. Hệ thống cung cấp đầy đủ các tính năng:

### Chức năng chính
- **Tổng quan**: Hiển thị dashboard với số dư, thu chi gần đây, biểu đồ thống kê.
- **Quản lý Ví**: Tạo và quản lý nhiều ví (tiền mặt, ngân hàng, ví điện tử).
- **Quỹ tiết kiệm**: Thiết lập và theo dõi các quỹ cho mục tiêu tài chính dài hạn.
- **Giao dịch**: Thêm, sửa, xóa giao dịch thu/chi, chuyển tiền giữa các ví.
- **Danh mục chi tiêu**: Phân loại giao dịch theo danh mục (ăn uống, mua sắm, hóa đơn...).
- **Nhóm ví**: Gom các ví vào nhóm để quản lý dễ dàng.
- **Ngân sách**: Đặt hạn mức chi tiêu cho từng danh mục, cảnh báo khi vượt mức.
- **Báo cáo tài chính**: Biểu đồ thu chi, so sánh ngân sách, phân tích xu hướng.
- **Quản lý tài khoản**: Đăng ký, đăng nhập, phân quyền, bảo mật bằng JWT.

Ứng dụng được thiết kế theo kiến trúc **RESTful API**, dễ dàng tích hợp với frontend hoặc mobile app.

## Công nghệ sử dụng
- **Ngôn ngữ**: Java 17+
- **Framework**: Spring Boot
- **Database**: MySQL / PostgreSQL
- **ORM**: Spring Data JPA
- **Migration**: Flyway
- **Bảo mật**: Spring Security + JWT

