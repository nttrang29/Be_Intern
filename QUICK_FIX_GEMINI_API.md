# 🔧 HƯỚNG DẪN NHANH: Cập nhật Gemini API Key

## ⚠️ Lỗi hiện tại
```
API key chưa được cấu hình. Vui lòng cập nhật API key trong application.properties
```

## ✅ Cách khắc phục (3 bước)

### Bước 1: Tạo API Key mới (2 phút)

1. **Mở trình duyệt và truy cập:**
   ```
   https://aistudio.google.com/app/apikey
   ```
   Hoặc:
   ```
   https://makersuite.google.com/app/apikey
   ```

2. **Đăng nhập** bằng tài khoản Google của bạn

3. **Click "Create API Key"** (hoặc "Get API Key")

4. **Chọn project:**
   - Chọn "Create API key in new project" (khuyến nghị)
   - Hoặc chọn project có sẵn

5. **Copy API Key** (sẽ có dạng: `AIzaSy...`)

### Bước 2: Cập nhật vào file (1 phút)

1. **Mở file:**
   ```
   final_project_I/backend/src/main/resources/application.properties
   ```

2. **Tìm dòng 109:**
   ```properties
   app.gemini.api-key=YOUR_NEW_GEMINI_API_KEY_HERE
   ```

3. **Thay thế `YOUR_NEW_GEMINI_API_KEY_HERE` bằng API key mới của bạn:**
   ```properties
   app.gemini.api-key=AIzaSyYourActualApiKeyHere123456789
   ```

4. **Lưu file** (Ctrl+S)

### Bước 3: Khởi động lại Backend (30 giây)

**Cách 1: Nếu đang chạy bằng Maven:**
```bash
cd final_project_I/backend
# Dừng server hiện tại (Ctrl+C)
# Sau đó chạy lại:
mvn spring-boot:run
```

**Cách 2: Nếu đang chạy bằng IDE:**
- Dừng ứng dụng (Stop button)
- Chạy lại (Run button)

## 🧪 Kiểm tra

Sau khi khởi động lại:
1. Mở ứng dụng web
2. Click vào icon chat (góc dưới bên phải)
3. Gửi tin nhắn: "Xin chào"
4. Nếu AI trả lời được → ✅ **Thành công!**
5. Nếu vẫn lỗi → Kiểm tra lại API key đã copy đúng chưa

## ❓ Vấn đề thường gặp

### Lỗi: "API key chưa được cấu hình"
- **Nguyên nhân:** Bạn chưa thay thế `YOUR_NEW_GEMINI_API_KEY_HERE`
- **Giải pháp:** Đảm bảo đã paste API key thực tế vào file

### Lỗi: "Your API key was reported as leaked"
- **Nguyên nhân:** API key cũ đã bị Google vô hiệu hóa
- **Giải pháp:** Tạo API key mới (không thể dùng lại key cũ)

### Lỗi: "API key không có quyền"
- **Nguyên nhân:** Chưa enable "Generative Language API"
- **Giải pháp:** 
  1. Vào: https://console.cloud.google.com/apis/library
  2. Tìm "Generative Language API"
  3. Click "Enable"

## 📝 Lưu ý

- ⚠️ **KHÔNG commit API key vào Git**
- ⚠️ **KHÔNG chia sẻ API key công khai**
- ✅ API key mới có thể mất vài phút để kích hoạt

## 🔗 Liên kết

- **Tạo API Key:** https://aistudio.google.com/app/apikey
- **Google Cloud Console:** https://console.cloud.google.com/
- **Hướng dẫn chi tiết:** Xem file `GEMINI_API_KEY_SETUP.md`

