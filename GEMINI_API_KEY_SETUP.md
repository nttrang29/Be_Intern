# Hướng dẫn tạo và cấu hình Google Gemini API Key

## ⚠️ Vấn đề hiện tại

API key hiện tại đã bị Google đánh dấu là **leaked** (bị rò rỉ) và không thể sử dụng nữa.

**Lỗi:** `Your API key was reported as leaked. Please use another API key.`

## 🔧 Cách tạo API key mới

### Bước 1: Truy cập Google AI Studio
1. Mở trình duyệt và truy cập: **https://aistudio.google.com/app/apikey**
2. Đăng nhập bằng tài khoản Google của bạn

### Bước 2: Tạo API Key mới
1. Click nút **"Create API Key"** (hoặc **"Get API Key"**)
2. Chọn một trong các tùy chọn:
   - **Create API key in new project**: Tạo project mới (khuyến nghị)
   - **Create API key in existing project**: Sử dụng project có sẵn
3. Click **"Create API key in new project"** (hoặc chọn project)
4. Đợi Google tạo API key (vài giây)
5. **Copy API key** (dạng: `AIzaSy...`)

### Bước 3: Cập nhật API key trong ứng dụng

1. Mở file: `final_project_I/backend/src/main/resources/application.properties`
2. Tìm dòng: `app.gemini.api-key=YOUR_NEW_GEMINI_API_KEY_HERE`
3. Thay thế `YOUR_NEW_GEMINI_API_KEY_HERE` bằng API key mới của bạn
4. Lưu file

**Ví dụ:**
```properties
app.gemini.api-key=AIzaSyYourNewApiKeyHere123456789
```

### Bước 4: Khởi động lại Backend
```bash
cd final_project_I/backend
mvn spring-boot:run
```

## 🔒 Bảo mật API Key

### ⚠️ QUAN TRỌNG - Tránh API key bị leak:

1. **KHÔNG commit API key vào Git:**
   - Thêm `application.properties` vào `.gitignore` (nếu chưa có)
   - Hoặc sử dụng file `application-local.properties` riêng (không commit)

2. **Sử dụng Environment Variables (Khuyến nghị):**
   ```properties
   app.gemini.api-key=${GEMINI_API_KEY:}
   ```
   Sau đó set environment variable:
   ```bash
   # Windows (PowerShell)
   $env:GEMINI_API_KEY="your-api-key-here"
   
   # Windows (CMD)
   set GEMINI_API_KEY=your-api-key-here
   
   # Linux/Mac
   export GEMINI_API_KEY=your-api-key-here
   ```

3. **Giới hạn API Key trong Google Cloud Console:**
   - Vào: https://console.cloud.google.com/apis/credentials
   - Chọn API key của bạn
   - Click "API restrictions"
   - Chỉ enable: **"Generative Language API"**
   - Click "Application restrictions" → Chọn "IP addresses" (nếu có server IP cố định)

4. **Không chia sẻ API key:**
   - Không post lên GitHub, GitLab, hoặc bất kỳ repository công khai nào
   - Không gửi qua email, chat, hoặc tin nhắn không bảo mật
   - Chỉ sử dụng trong môi trường development/production của bạn

## 🧪 Kiểm tra API Key hoạt động

Sau khi cập nhật API key, test lại chatbot:
1. Mở ứng dụng
2. Click vào icon chat (góc dưới bên phải)
3. Gửi một câu hỏi đơn giản: "Xin chào"
4. Nếu AI trả lời được → API key hoạt động ✅
5. Nếu vẫn lỗi 403 → Kiểm tra lại API key hoặc tạo key mới

## 📝 Lưu ý

- API key mới có thể mất vài phút để kích hoạt
- Nếu vẫn gặp lỗi, thử tạo API key mới hoặc kiểm tra quota trong Google Cloud Console
- Đảm bảo đã enable "Generative Language API" trong Google Cloud Console

## 🔗 Liên kết hữu ích

- **Tạo API Key:** https://aistudio.google.com/app/apikey
- **Google Cloud Console:** https://console.cloud.google.com/
- **API Documentation:** https://ai.google.dev/docs

