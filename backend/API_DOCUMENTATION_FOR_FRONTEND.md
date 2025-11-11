# 📚 BACKEND API DOCUMENTATION - FINANCE APP
## Tài liệu đầy đủ cho Frontend Integration

---

## 🌐 **BASE CONFIGURATION**

```javascript
const API_BASE_URL = "http://localhost:8080";

// Axios configuration (recommended)
import axios from 'axios';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  }
});

// Interceptor để tự động thêm JWT token
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Interceptor để handle refresh token khi 401
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401 && !error.config._retry) {
      error.config._retry = true;
      try {
        const refreshToken = localStorage.getItem('refreshToken');
        const { data } = await axios.post(`${API_BASE_URL}/auth/refresh`, {
          refreshToken
        });
        localStorage.setItem('accessToken', data.accessToken);
        error.config.headers.Authorization = `Bearer ${data.accessToken}`;
        return apiClient(error.config);
      } catch (refreshError) {
        localStorage.clear();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
);
```

---

## 🔐 **MODULE 1: AUTHENTICATION**

### **1.1. Đăng ký tài khoản**

**Endpoint:** `POST /auth/register`  
**Auth:** None (Public)

**Request:**
```javascript
{
  "fullName": "Nguyễn Văn A",
  "email": "user@example.com",
  "password": "Password@123",
  "confirmPassword": "Password@123",
  "recaptchaToken": "03AGdBq..." // From Google reCAPTCHA
}
```

**Response Success (200):**
```javascript
{
  "message": "Đăng ký thành công. Vui lòng kiểm tra email để xác minh tài khoản."
}
```

**Response Error (400):**
```javascript
{
  "error": "Email đã được sử dụng và tài khoản đã được kích hoạt. Vui lòng đăng nhập."
}
// OR
{
  "error": "Mật khẩu phải ≥8 ký tự, có chữ hoa, thường, số, ký tự đặc biệt"
}
```

**Validation Rules:**
- Password: ≥8 chars, có uppercase, lowercase, number, special char
- Email: Valid email format
- confirmPassword phải match password

---

### **1.2. Xác minh email**

**Endpoint:** `POST /auth/verify`  
**Auth:** None (Public)

**Request:**
```javascript
{
  "email": "user@example.com",
  "code": "123456" // 6-digit code từ email
}
```

**Response Success (200):**
```javascript
{
  "message": "Xác minh thành công",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Frontend Action:**
```javascript
// Lưu tokens
localStorage.setItem('accessToken', data.accessToken);
localStorage.setItem('refreshToken', data.refreshToken);
// Redirect to dashboard
```

---

### **1.3. Đăng nhập**

**Endpoint:** `POST /auth/login`  
**Auth:** None (Public)

**Request:**
```javascript
{
  "email": "user@example.com",
  "password": "Password@123"
}
```

**Response Success (200):**
```javascript
{
  "message": "Đăng nhập thành công",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "userId": 1,
    "fullName": "Nguyễn Văn A",
    "email": "user@example.com",
    "provider": "local",
    "avatar": null,
    "enabled": true
  }
}
```

**Response Error (400):**
```javascript
{
  "error": "Tài khoản hoặc mật khẩu không chính xác."
}
// OR (Google user chưa set password)
{
  "error": "Tài khoản này đăng nhập bằng Google. Vui lòng đăng nhập bằng Google hoặc đặt mật khẩu trong phần hồ sơ."
}
```

---

### **1.4. Đăng nhập bằng Google OAuth2**

**Flow:**
```javascript
// Step 1: Redirect user to Google
window.location.href = `${API_BASE_URL}/auth/oauth2/authorization/google`;

// Step 2: Google redirects back to: http://localhost:3000/oauth/callback?token=JWT_TOKEN

// Step 3: Extract token
const urlParams = new URLSearchParams(window.location.search);
const token = urlParams.get('token');
localStorage.setItem('accessToken', token);
```

**Backend Response:**
- User mới → Tạo account với `passwordHash = null`, `enabled = true`
- User cũ → Update avatar và enabled status

---

### **1.5. Quên mật khẩu**

**Endpoint:** `POST /auth/forgot-password`  
**Auth:** None (Public)

**Request:**
```javascript
{
  "email": "user@example.com"
}
```

**Response Success (200):**
```javascript
{
  "message": "Mã xác thực đã gửi đến email"
}
```

---

### **1.6. Reset mật khẩu**

**Endpoint:** `POST /auth/reset-password`  
**Auth:** None (Public)

**Request:**
```javascript
{
  "email": "user@example.com",
  "Mã xác thực": "123456", // OTP từ email
  "newPassword": "NewPassword@123",
  "confirmPassword": "NewPassword@123"
}
```

**Response Success (200):**
```javascript
{
  "message": "Đổi mật khẩu thành công"
}
```

---

### **1.7. Refresh Token**

**Endpoint:** `POST /auth/refresh`  
**Auth:** None (Public)

**Request:**
```javascript
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response Success (200):**
```javascript
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "message": "Làm mới token thành công"
}
```

---

## 👤 **MODULE 2: PROFILE**

### **2.1. Xem thông tin profile**

**Endpoint:** `GET /profile`  
**Auth:** Required (JWT)

**Response Success (200):**
```javascript
{
  "user": {
    "userId": 1,
    "fullName": "Nguyễn Văn A",
    "email": "user@example.com",
    "provider": "local", // hoặc "google"
    "avatar": "https://...",
    "enabled": true
  }
}
```

---

### **2.2. Cập nhật profile**

**Endpoint:** `POST /profile/update`  
**Auth:** Required (JWT)

**Request:**
```javascript
{
  "fullName": "Nguyễn Văn B",
  "avatar": "https://..." // hoặc base64 string
}
```

**Response Success (200):**
```javascript
{
  "message": "Cập nhật profile thành công",
  "user": { /* user object */ }
}
```

---

### **2.3. Đổi mật khẩu**

**Endpoint:** `POST /profile/change-password`  
**Auth:** Required (JWT)

**Request:**
```javascript
// Trường hợp 1: Google user đặt password lần đầu (không cần oldPassword)
{
  "newPassword": "NewPassword@123",
  "confirmPassword": "NewPassword@123"
}

// Trường hợp 2: Đổi password (cần oldPassword)
{
  "oldPassword": "OldPassword@123",
  "newPassword": "NewPassword@123",
  "confirmPassword": "NewPassword@123"
}
```

**Response Success (200):**
```javascript
{
  "message": "Đặt mật khẩu thành công. Bây giờ bạn có thể đăng nhập bằng email và mật khẩu."
}
// OR
{
  "message": "Đổi mật khẩu thành công"
}
```

---

## 💰 **MODULE 3: WALLET MANAGEMENT**

### **3.1. Tạo ví mới**

**Endpoint:** `POST /wallets/create`  
**Auth:** Required (JWT)

**Request:**
```javascript
{
  "walletName": "Ví tiền mặt",
  "currencyCode": "VND", // VND, USD, EUR, etc
  "initialBalance": 1000000.0,
  "description": "Ví tiền mặt cá nhân",
  "setAsDefault": true // optional
}
```

**Response Success (200):**
```javascript
{
  "message": "Tạo ví thành công",
  "wallet": {
    "walletId": 1,
    "walletName": "Ví tiền mặt",
    "currencyCode": "VND",
    "balance": 1000000.00,
    "description": "Ví tiền mặt cá nhân",
    "isDefault": true,
    "createdAt": "2024-11-12T10:30:00",
    "updatedAt": "2024-11-12T10:30:00"
  }
}
```

---

### **3.2. Lấy danh sách ví**

**Endpoint:** `GET /wallets`  
**Auth:** Required (JWT)

**Response Success (200):**
```javascript
{
  "wallets": [
    {
      "walletId": 1,
      "walletName": "Ví gia đình",
      "currencyCode": "VND",
      "balance": 5000000.00,
      "description": "Ví chung vợ chồng",
      "myRole": "OWNER", // hoặc "MEMBER"
      "ownerId": 1,
      "ownerName": "Nguyễn Văn A",
      "totalMembers": 2,
      "isDefault": true,
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-20T14:45:00"
    }
  ],
  "total": 1
}
```

**Frontend Display:**
```javascript
wallets.forEach(wallet => {
  // Hiển thị badge nếu là shared wallet
  if (wallet.totalMembers > 1) {
    showBadge("Shared with " + (wallet.totalMembers - 1) + " người");
  }
  
  // Hiển thị role
  if (wallet.myRole === "MEMBER") {
    showTag("Ví được chia sẻ");
    disableEditButton(); // Member không sửa được
  }
});
```

---

### **3.3. Xem chi tiết ví**

**Endpoint:** `GET /wallets/{walletId}`  
**Auth:** Required (JWT)

**Response Success (200):**
```javascript
{
  "wallet": {
    "walletId": 1,
    "walletName": "Ví tiền mặt",
    "currencyCode": "VND",
    "balance": 5000000.00,
    "description": "Ví cá nhân",
    "isDefault": true,
    "createdAt": "2024-11-12T10:30:00",
    "updatedAt": "2024-11-12T14:45:00"
  }
}
```

---

### **3.4. Cập nhật ví**

**Endpoint:** `PUT /wallets/{walletId}`  
**Auth:** Required (JWT - OWNER only)

**Request:**
```javascript
// Chỉ sửa tên và mô tả
{
  "walletName": "Ví mới",
  "description": "Updated description"
}

// Sửa cả balance (CHỈ khi chưa có transaction)
{
  "walletName": "Ví mới",
  "description": "Updated",
  "balance": 2000000.00
}
```

**Response Success (200):**
```javascript
{
  "message": "Cập nhật ví thành công",
  "wallet": { /* updated wallet */ }
}
```

**Response Error (400):**
```javascript
{
  "error": "Không thể chỉnh sửa số dư khi ví đã có giao dịch. Ví này có 45 giao dịch. Số dư chỉ có thể thay đổi thông qua giao dịch hoặc bạn có thể xóa ví."
}
```

**Frontend Logic:**
```javascript
// Kiểm tra trước khi cho edit balance
if (wallet.transactionCount > 0) {
  disableBalanceField();
  showTooltip("Không thể sửa số dư khi đã có giao dịch");
}
```

---

### **3.5. Xóa ví**

**Endpoint:** `DELETE /wallets/{walletId}`  
**Auth:** Required (JWT - OWNER only)

**Response Success (200):**
```javascript
{
  "message": "Xóa ví thành công",
  "deletedWallet": {
    "deletedWalletId": 1,
    "deletedWalletName": "Ví cũ",
    "balance": 100000,
    "currencyCode": "VND",
    "transactionsDeleted": 45,
    "membersRemoved": 3,
    "wasDefault": true,
    "newDefaultWalletId": 2,
    "newDefaultWalletName": "Ví mới"
  }
}
```

**Frontend Action:**
```javascript
// Show confirmation dialog
const confirmed = await showConfirmDialog({
  title: "Xác nhận xóa ví",
  message: `Bạn có chắc muốn xóa ví "${wallet.walletName}"?`,
  warnings: [
    wallet.balance > 0 ? `⚠️ Ví còn ${formatMoney(wallet.balance)}` : null,
    `❌ ${wallet.transactionCount} giao dịch sẽ bị xóa vĩnh viễn`,
    `❌ Hành động này không thể hoàn tác`
  ]
});

if (confirmed) {
  await apiClient.delete(`/wallets/${walletId}`);
  refreshWalletList();
}
```

---

### **3.6. Đặt ví mặc định**

**Endpoint:** `PATCH /wallets/{walletId}/set-default`  
**Auth:** Required (JWT)

**Response Success (200):**
```javascript
{
  "message": "Đặt ví mặc định thành công"
}
```

---

## 👨‍👩‍👧‍👦 **MODULE 4: SHARED WALLET**

### **4.1. Chia sẻ ví**

**Endpoint:** `POST /wallets/{walletId}/share`  
**Auth:** Required (JWT - OWNER only)

**Request:**
```javascript
{
  "email": "wife@example.com"
}
```

**Response Success (200):**
```javascript
{
  "message": "Chia sẻ ví thành công",
  "member": {
    "memberId": 5,
    "userId": 3,
    "fullName": "Nguyễn Thị B",
    "email": "wife@example.com",
    "avatar": "https://...",
    "role": "MEMBER",
    "joinedAt": "2024-11-12T10:30:00"
  }
}
```

---

### **4.2. Xem danh sách members**

**Endpoint:** `GET /wallets/{walletId}/members`  
**Auth:** Required (JWT - có quyền truy cập ví)

**Response Success (200):**
```javascript
{
  "members": [
    {
      "memberId": 1,
      "userId": 1,
      "fullName": "Nguyễn Văn A",
      "email": "user@example.com",
      "avatar": null,
      "role": "OWNER",
      "joinedAt": "2024-01-15T10:30:00"
    },
    {
      "memberId": 5,
      "userId": 3,
      "fullName": "Nguyễn Thị B",
      "email": "wife@example.com",
      "avatar": null,
      "role": "MEMBER",
      "joinedAt": "2024-03-15T10:30:00"
    }
  ],
  "total": 2
}
```

---

### **4.3. Xóa member**

**Endpoint:** `DELETE /wallets/{walletId}/members/{memberUserId}`  
**Auth:** Required (JWT - OWNER only)

**Response Success (200):**
```javascript
{
  "message": "Xóa thành viên thành công"
}
```

---

### **4.4. Rời khỏi ví**

**Endpoint:** `POST /wallets/{walletId}/leave`  
**Auth:** Required (JWT - MEMBER only)

**Response Success (200):**
```javascript
{
  "message": "Bạn đã rời khỏi ví thành công"
}
```

---

### **4.5. Kiểm tra quyền truy cập**

**Endpoint:** `GET /wallets/{walletId}/access`  
**Auth:** Required (JWT)

**Response Success (200):**
```javascript
{
  "hasAccess": true,
  "isOwner": false,
  "role": "MEMBER" // "OWNER", "MEMBER", hoặc "NONE"
}
```

**Frontend Usage:**
```javascript
const access = await apiClient.get(`/wallets/${walletId}/access`);

if (access.data.isOwner) {
  showEditButton();
  showDeleteButton();
  showShareButton();
} else if (access.data.role === "MEMBER") {
  hideEditButton();
  hideDeleteButton();
  showLeaveButton();
} else {
  showError("Bạn không có quyền truy cập ví này");
}
```

---

## 💸 **MODULE 5: TRANSACTIONS**

### **5.1. Tạo chi tiêu**

**Endpoint:** `POST /transactions/expense`  
**Auth:** Required (JWT - OWNER hoặc MEMBER)

**Request:**
```javascript
{
  "walletId": 1,
  "categoryId": 5,
  "amount": 50000.00,
  "transactionDate": "2024-11-12T14:30:00",
  "note": "Ăn trưa với đồng nghiệp",
  "imageUrl": "https://..." // optional
}
```

**Response Success (200):**
```javascript
{
  "message": "Thêm chi tiêu thành công",
  "transaction": {
    "transactionId": 100,
    "amount": 50000.00,
    "transactionDate": "2024-11-12T14:30:00",
    "note": "Ăn trưa",
    "wallet": { /* wallet object */ },
    "category": { /* category object */ },
    "transactionType": { /* type object */ }
  }
}
```

**Response Error (400):**
```javascript
{
  "error": "Số dư không đủ. Số dư hiện tại: 30000 VND, Số tiền chi tiêu: 50000 VND"
}
```

---

### **5.2. Tạo thu nhập**

**Endpoint:** `POST /transactions/income`  
**Auth:** Required (JWT - OWNER hoặc MEMBER)

**Request:** (giống expense)

**Response Success (200):**
```javascript
{
  "message": "Thêm thu nhập thành công",
  "transaction": { /* transaction object */ }
}
```

---

## 🔀 **MODULE 6: WALLET MERGE (with Currency Conversion)**

### **6.1. Lấy danh sách ví có thể gộp**

**Endpoint:** `GET /wallets/{sourceWalletId}/merge-candidates`  
**Auth:** Required (JWT - OWNER)

**Response Success (200):**
```javascript
{
  "candidateWallets": [
    {
      "walletId": 3,
      "walletName": "Ví ngân hàng",
      "currencyCode": "VND",
      "balance": 10000000,
      "transactionCount": 30,
      "isDefault": false,
      "canMerge": true,
      "reason": null
    },
    {
      "walletId": 5,
      "walletName": "Ví EUR",
      "currencyCode": "EUR",
      "balance": 500,
      "transactionCount": 10,
      "isDefault": false,
      "canMerge": true,
      "reason": null
    }
  ],
  "ineligibleWallets": [
    {
      "walletId": 7,
      "walletName": "Ví shared",
      "currencyCode": "VND",
      "balance": 2000000,
      "canMerge": false,
      "reason": "Ví đã được chia sẻ với 2 người khác"
    }
  ],
  "total": 2
}
```

**Frontend Logic:**
```javascript
// Hiển thị candidates
candidateWallets.forEach(wallet => {
  // Nếu khác currency → show warning
  if (wallet.currencyCode !== sourceWallet.currencyCode) {
    showWarningIcon("⚠️ Khác loại tiền tệ");
  }
});
```

---

### **6.2. Preview merge với currency conversion**

**Endpoint:** `GET /wallets/{targetWalletId}/merge-preview?sourceWalletId=X&targetCurrency=VND`  
**Auth:** Required (JWT - OWNER)

**Query Params:**
- `sourceWalletId` (required): ID ví nguồn
- `targetCurrency` (required): Loại tiền sau merge ("VND", "USD", etc)

**Response Success (200):**
```javascript
{
  "preview": {
    // Source wallet info
    "sourceWalletId": 1,
    "sourceWalletName": "Ví USD",
    "sourceCurrency": "USD",
    "sourceBalance": 1000.00,
    "sourceTransactionCount": 15,
    "sourceIsDefault": false,
    
    // Target wallet info
    "targetWalletId": 3,
    "targetWalletName": "Ví ngân hàng",
    "targetCurrency": "VND",
    "targetBalance": 10000000,
    "targetTransactionCount": 30,
    
    // Result after merge
    "finalWalletName": "Ví ngân hàng",
    "finalCurrency": "VND",
    "finalBalance": 34350000, // 10,000,000 + (1,000 * 24,350)
    "totalTransactions": 45,
    "willTransferDefaultFlag": false,
    
    // Warnings
    "warnings": [
      "Ví 'Ví USD' sẽ bị xóa vĩnh viễn",
      "15 giao dịch từ Ví USD sẽ được chuyển đổi sang VND theo tỷ giá: 1 USD = 24350.0 VND",
      "Bạn vẫn có thể xem số tiền gốc (USD) của mỗi giao dịch",
      "Tỷ giá có thể thay đổi. Tỷ giá hiển thị chỉ mang tính tham khảo",
      "Hành động này KHÔNG THỂ hoàn tác"
    ],
    "canProceed": true
  }
}
```

**Frontend Flow:**
```javascript
// Step 1: User chọn target wallet
// Step 2: Nếu khác currency → Show dialog chọn targetCurrency
let targetCurrency;
if (sourceWallet.currencyCode !== targetWallet.currencyCode) {
  targetCurrency = await showCurrencySelectionDialog({
    options: [
      {
        value: sourceWallet.currencyCode,
        label: `Chuyển sang ${sourceWallet.currencyCode}`,
        description: `Chuyển đổi ${targetWallet.currencyCode} → ${sourceWallet.currencyCode}`
      },
      {
        value: targetWallet.currencyCode,
        label: `Giữ ${targetWallet.currencyCode}`,
        description: `Chuyển đổi ${sourceWallet.currencyCode} → ${targetWallet.currencyCode}`
      }
    ]
  });
} else {
  targetCurrency = sourceWallet.currencyCode; // Cùng currency
}

// Step 3: Load preview
const preview = await apiClient.get(
  `/wallets/${targetWalletId}/merge-preview`,
  {
    params: {
      sourceWalletId: sourceWalletId,
      targetCurrency: targetCurrency
    }
  }
);

// Step 4: Show preview với warnings
showPreview(preview.data.preview);
```

---

### **6.3. Thực hiện merge**

**Endpoint:** `POST /wallets/{targetWalletId}/merge`  
**Auth:** Required (JWT - OWNER)

**Request:**
```javascript
{
  "sourceWalletId": 1,
  "targetCurrency": "VND" // REQUIRED
}
```

**Response Success (200):**
```javascript
{
  "success": true,
  "message": "Gộp ví thành công. Đã chuyển đổi 15 giao dịch từ USD sang VND",
  "result": {
    "targetWalletId": 3,
    "targetWalletName": "Ví ngân hàng",
    "finalBalance": 34350000,
    "finalCurrency": "VND",
    "mergedTransactions": 15,
    "sourceWalletName": "Ví USD",
    "wasDefaultTransferred": false,
    "mergeHistoryId": 1,
    "mergedAt": "2024-11-12T14:30:00"
  }
}
```

**Frontend Action:**
```javascript
// After success
showToast("Gộp ví thành công!");
refreshWalletList();
navigateToWallet(result.targetWalletId);
```

---

## 💱 **MODULE 7: MONEY TRANSFER**

### **7.1. Chuyển tiền giữa các ví**

**Endpoint:** `POST /wallets/transfer`  
**Auth:** Required (JWT - OWNER hoặc MEMBER của cả 2 ví)

**Request:**
```javascript
{
  "fromWalletId": 1,
  "toWalletId": 3,
  "amount": 500000.00,
  "categoryId": 10, // Category "Chuyển khoản nội bộ"
  "note": "Chuyển tiền tiết kiệm" // optional
}
```

**Response Success (200):**
```javascript
{
  "message": "Chuyển tiền thành công",
  "transfer": {
    "amount": 500000,
    "currencyCode": "VND",
    "transferredAt": "2024-11-12T15:00:00",
    "note": "Chuyển tiền tiết kiệm",
    
    // From wallet
    "fromWalletId": 1,
    "fromWalletName": "Ví tiền mặt",
    "fromWalletBalanceBefore": 2000000,
    "fromWalletBalanceAfter": 1500000,
    "expenseTransactionId": 101,
    
    // To wallet
    "toWalletId": 3,
    "toWalletName": "Ví ngân hàng",
    "toWalletBalanceBefore": 10000000,
    "toWalletBalanceAfter": 10500000,
    "incomeTransactionId": 102
  }
}
```

**Response Error (400):**
```javascript
{
  "error": "Số dư ví nguồn không đủ. Số dư hiện tại: 300000 VND, Số tiền chuyển: 500000 VND"
}
// OR
{
  "error": "Chỉ có thể chuyển tiền giữa các ví cùng loại tiền tệ. Ví nguồn: USD, Ví đích: VND"
}
```

**Frontend Flow:**
```javascript
// Validation trước khi submit
if (fromWallet.currencyCode !== toWallet.currencyCode) {
  showError("Chỉ có thể chuyển tiền giữa các ví cùng loại tiền tệ");
  return;
}

if (amount > fromWallet.balance) {
  showError("Số dư không đủ");
  return;
}

// Submit
const result = await apiClient.post('/wallets/transfer', {
  fromWalletId,
  toWalletId,
  amount,
  categoryId,
  note
});

// Success
showToast("Chuyển tiền thành công");
updateWalletBalance(fromWalletId, result.transfer.fromWalletBalanceAfter);
updateWalletBalance(toWalletId, result.transfer.toWalletBalanceAfter);
```

---

## 📊 **COMMON PATTERNS & BEST PRACTICES**

### **Error Handling**

```javascript
try {
  const response = await apiClient.post('/endpoint', data);
  // Success
  showToast(response.data.message);
} catch (error) {
  if (error.response) {
    // Server responded with error status
    const errorMsg = error.response.data.error || 
                     error.response.data.message ||
                     'Đã có lỗi xảy ra';
    showError(errorMsg);
  } else if (error.request) {
    // Request made but no response
    showError('Không thể kết nối đến server');
  } else {
    // Something else happened
    showError('Đã có lỗi xảy ra');
  }
}
```

---

### **Loading States**

```javascript
const [loading, setLoading] = useState(false);

const handleSubmit = async () => {
  setLoading(true);
  try {
    const response = await apiClient.post('/endpoint', data);
    // Handle success
  } catch (error) {
    // Handle error
  } finally {
    setLoading(false);
  }
};
```

---

### **Form Validation**

```javascript
// Password validation
const validatePassword = (password) => {
  if (password.length < 8) return "Mật khẩu phải ≥8 ký tự";
  if (!/[A-Z]/.test(password)) return "Phải có chữ hoa";
  if (!/[a-z]/.test(password)) return "Phải có chữ thường";
  if (!/[0-9]/.test(password)) return "Phải có số";
  if (!/[!@#$%^&*()_+\-=\[\]{}|;:,.<>?]/.test(password)) {
    return "Phải có ký tự đặc biệt";
  }
  return null;
};

// Currency validation
const validateCurrency = (code) => {
  return /^[A-Z]{3}$/.test(code);
};

// Amount validation
const validateAmount = (amount) => {
  return amount > 0;
};
```

---

## 🎯 **PERMISSION MATRIX**

| Action | OWNER | MEMBER | No Access |
|--------|:-----:|:------:|:---------:|
| View wallet | ✅ | ✅ | ❌ |
| Create transaction | ✅ | ✅ | ❌ |
| Update wallet | ✅ | ❌ | ❌ |
| Delete wallet | ✅ | ❌ | ❌ |
| Share wallet | ✅ | ❌ | ❌ |
| Remove member | ✅ | ❌ | ❌ |
| Leave wallet | ❌ | ✅ | ❌ |
| Transfer money FROM | ✅ | ✅ | ❌ |
| Merge wallet | ✅ | ❌ | ❌ |

**Frontend Implementation:**
```javascript
const canEdit = (wallet) => wallet.myRole === "OWNER";
const canDelete = (wallet) => wallet.myRole === "OWNER";
const canShare = (wallet) => wallet.myRole === "OWNER";
const canLeave = (wallet) => wallet.myRole === "MEMBER";
const canCreateTransaction = (wallet) => ["OWNER", "MEMBER"].includes(wallet.myRole);
const canTransferFrom = (wallet) => ["OWNER", "MEMBER"].includes(wallet.myRole);
```

---

## 🔄 **TYPICAL USER FLOWS**

### **Flow 1: Đăng ký và xác minh**
```javascript
// Step 1: Register
await apiClient.post('/auth/register', {
  fullName, email, password, confirmPassword, recaptchaToken
});

// Step 2: Show "Check your email" message

// Step 3: User nhập code
await apiClient.post('/auth/verify', { email, code });

// Step 4: Save tokens & redirect
localStorage.setItem('accessToken', data.accessToken);
localStorage.setItem('refreshToken', data.refreshToken);
navigate('/dashboard');
```

---

### **Flow 2: Tạo và quản lý ví**
```javascript
// Step 1: Create wallet
await apiClient.post('/wallets/create', {
  walletName: "Ví tiền mặt",
  currencyCode: "VND",
  initialBalance: 1000000
});

// Step 2: Get wallets
const { data } = await apiClient.get('/wallets');
setWallets(data.wallets);

// Step 3: Update wallet
await apiClient.put(`/wallets/${walletId}`, {
  walletName: "Ví mới",
  description: "Updated"
});

// Step 4: Delete wallet (with confirmation)
if (confirmed) {
  await apiClient.delete(`/wallets/${walletId}`);
}
```

---

### **Flow 3: Tạo transaction**
```javascript
// Step 1: Get wallet info
const wallet = await apiClient.get(`/wallets/${walletId}`);

// Step 2: Validate balance (for expense)
if (type === 'expense' && amount > wallet.data.wallet.balance) {
  showError("Số dư không đủ");
  return;
}

// Step 3: Create transaction
await apiClient.post('/transactions/expense', {
  walletId,
  categoryId,
  amount,
  transactionDate: new Date(),
  note
});

// Step 4: Refresh wallet balance
refreshWallet(walletId);
```

---

### **Flow 4: Merge ví với currency conversion**
```javascript
// Step 1: Click "Gộp" on source wallet
navigate(`/wallets/${sourceWalletId}/merge`);

// Step 2: Get merge candidates
const candidates = await apiClient.get(
  `/wallets/${sourceWalletId}/merge-candidates`
);

// Step 3: User chọn target wallet
const targetWallet = selectedWallet;

// Step 4: Nếu khác currency → Chọn targetCurrency
let targetCurrency;
if (sourceWallet.currencyCode !== targetWallet.currencyCode) {
  targetCurrency = await showCurrencyDialog([
    sourceWallet.currencyCode,
    targetWallet.currencyCode
  ]);
} else {
  targetCurrency = sourceWallet.currencyCode;
}

// Step 5: Load preview
const preview = await apiClient.get(
  `/wallets/${targetWallet.walletId}/merge-preview`,
  {
    params: { sourceWalletId, targetCurrency }
  }
);

// Step 6: Show preview với checkbox confirmation
showPreview(preview.data.preview);

// Step 7: User confirms
await apiClient.post(`/wallets/${targetWallet.walletId}/merge`, {
  sourceWalletId,
  targetCurrency
});

// Step 8: Success
showToast("Gộp ví thành công!");
navigate('/wallets');
```

---

### **Flow 5: Share wallet**
```javascript
// Step 1: OWNER clicks "Share"
// Step 2: Input email
const email = await showShareDialog();

// Step 3: Share wallet
await apiClient.post(`/wallets/${walletId}/share`, { email });

// Step 4: View members
const members = await apiClient.get(`/wallets/${walletId}/members`);
showMemberList(members.data.members);
```

---

### **Flow 6: Transfer money**
```javascript
// Step 1: Select from & to wallets
// Step 2: Validate same currency
if (fromWallet.currencyCode !== toWallet.currencyCode) {
  showError("Chỉ chuyển được giữa ví cùng loại tiền tệ");
  return;
}

// Step 3: Transfer
await apiClient.post('/wallets/transfer', {
  fromWalletId,
  toWalletId,
  amount,
  categoryId,
  note
});

// Step 4: Update balances
refreshWalletList();
```

---

## 🚨 **ERROR CODES & MESSAGES**

### **Common Error Responses:**

```javascript
// 400 Bad Request - Validation error
{
  "error": "Số tiền phải lớn hơn 0"
}

// 401 Unauthorized - No token or invalid token
{
  "error": "Unauthorized"
}

// 403 Forbidden - No permission
{
  "error": "Bạn không có quyền truy cập ví này"
}

// 404 Not Found
{
  "error": "Ví không tồn tại"
}

// 500 Internal Server Error
{
  "error": "Lỗi máy chủ nội bộ: ..."
}
```

**Frontend Error Handler:**
```javascript
const handleApiError = (error) => {
  const status = error.response?.status;
  const message = error.response?.data?.error || 
                  error.response?.data?.message || 
                  'Đã có lỗi xảy ra';
  
  switch(status) {
    case 400:
      showError(message); // Validation error
      break;
    case 401:
      localStorage.clear();
      navigate('/login');
      showError("Phiên đăng nhập hết hạn");
      break;
    case 403:
      showError("Bạn không có quyền thực hiện hành động này");
      break;
    case 404:
      showError("Không tìm thấy dữ liệu");
      break;
    case 500:
      showError("Lỗi server. Vui lòng thử lại sau");
      break;
    default:
      showError(message);
  }
};
```

---

## 📝 **DATA MODELS**

### **User Object:**
```typescript
interface User {
  userId: number;
  fullName: string;
  email: string;
  provider: "local" | "google";
  avatar?: string;
  enabled: boolean;
}
```

### **Wallet Object:**
```typescript
interface Wallet {
  walletId: number;
  walletName: string;
  currencyCode: string; // "VND", "USD", "EUR"
  balance: number;
  description?: string;
  isDefault: boolean;
  createdAt: string; // ISO datetime
  updatedAt: string;
}
```

### **SharedWallet Object:**
```typescript
interface SharedWallet extends Wallet {
  myRole: "OWNER" | "MEMBER";
  ownerId: number;
  ownerName: string;
  totalMembers: number;
}
```

### **Transaction Object:**
```typescript
interface Transaction {
  transactionId: number;
  amount: number;
  transactionDate: string;
  note?: string;
  imageUrl?: string;
  
  // For converted transactions (from merge)
  originalAmount?: number;
  originalCurrency?: string;
  exchangeRate?: number;
  mergeDate?: string;
  
  // Relations
  wallet: Wallet;
  category: Category;
  transactionType: TransactionType;
  user: User;
}
```

**Display Logic:**
```javascript
const displayTransaction = (tx) => {
  if (tx.originalAmount && tx.originalCurrency) {
    // Transaction đã được convert
    return (
      <div>
        <div>{formatMoney(tx.amount)} {tx.wallet.currencyCode}</div>
        <div className="conversion-info">
          Đã chuyển đổi từ: {tx.originalAmount} {tx.originalCurrency}
          <br/>
          Tỷ giá: 1 {tx.originalCurrency} = {tx.exchangeRate} {tx.wallet.currencyCode}
          <br/>
          Ngày gộp ví: {formatDate(tx.mergeDate)}
        </div>
      </div>
    );
  } else {
    // Transaction bình thường
    return <div>{formatMoney(tx.amount)} {tx.wallet.currencyCode}</div>;
  }
};
```

---

## 🔒 **SECURITY & AUTHENTICATION**

### **JWT Token Management:**

```javascript
// Store tokens
localStorage.setItem('accessToken', token);
localStorage.setItem('refreshToken', refreshToken);

// Get tokens
const getAccessToken = () => localStorage.getItem('accessToken');
const getRefreshToken = () => localStorage.getItem('refreshToken');

// Clear tokens (logout)
const logout = () => {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  navigate('/login');
};

// Check if logged in
const isAuthenticated = () => {
  return !!getAccessToken();
};
```

### **Protected Routes:**

```javascript
// React Router example
import { Navigate } from 'react-router-dom';

const ProtectedRoute = ({ children }) => {
  if (!isAuthenticated()) {
    return <Navigate to="/login" />;
  }
  return children;
};

// Usage
<Route path="/wallets" element={
  <ProtectedRoute>
    <WalletList />
  </ProtectedRoute>
} />
```

---

## 📋 **COMPLETE API ENDPOINT LIST**

### **Authentication (Public):**
- `POST /auth/register` - Đăng ký
- `POST /auth/verify` - Xác minh email
- `POST /auth/login` - Đăng nhập
- `POST /auth/forgot-password` - Quên mật khẩu
- `POST /auth/reset-password` - Reset mật khẩu
- `POST /auth/refresh` - Refresh token
- `GET /auth/oauth2/authorization/google` - Google OAuth

### **Profile (Protected):**
- `GET /profile` - Xem profile
- `POST /profile/update` - Cập nhật profile
- `POST /profile/change-password` - Đổi mật khẩu

### **Wallets (Protected):**
- `POST /wallets/create` - Tạo ví
- `GET /wallets` - Lấy danh sách ví
- `GET /wallets/{id}` - Xem chi tiết ví
- `PUT /wallets/{id}` - Cập nhật ví
- `DELETE /wallets/{id}` - Xóa ví
- `PATCH /wallets/{id}/set-default` - Đặt ví mặc định

### **Shared Wallet (Protected):**
- `POST /wallets/{id}/share` - Chia sẻ ví
- `GET /wallets/{id}/members` - Xem members
- `DELETE /wallets/{id}/members/{userId}` - Xóa member
- `POST /wallets/{id}/leave` - Rời khỏi ví
- `GET /wallets/{id}/access` - Kiểm tra quyền

### **Transactions (Protected):**
- `POST /transactions/expense` - Tạo chi tiêu
- `POST /transactions/income` - Tạo thu nhập

### **Wallet Merge (Protected - OWNER only):**
- `GET /wallets/{id}/merge-candidates` - Lấy ví có thể gộp
- `GET /wallets/{id}/merge-preview?sourceWalletId=X&targetCurrency=VND` - Preview merge
- `POST /wallets/{id}/merge` - Thực hiện merge

### **Money Transfer (Protected):**
- `POST /wallets/transfer` - Chuyển tiền giữa ví

---

## ⚡ **QUICK START GUIDE**

### **1. Setup API Client**
```javascript
// src/api/client.js
import axios from 'axios';

export const apiClient = axios.create({
  baseURL: 'http://localhost:8080',
  headers: { 'Content-Type': 'application/json' }
});

// Add interceptors (xem phần Base Configuration)
```

### **2. Create API Services**
```javascript
// src/api/authService.js
import { apiClient } from './client';

export const authService = {
  register: (data) => apiClient.post('/auth/register', data),
  login: (data) => apiClient.post('/auth/login', data),
  verify: (data) => apiClient.post('/auth/verify', data),
  // ... other methods
};

// src/api/walletService.js
export const walletService = {
  getAll: () => apiClient.get('/wallets'),
  getById: (id) => apiClient.get(`/wallets/${id}`),
  create: (data) => apiClient.post('/wallets/create', data),
  update: (id, data) => apiClient.put(`/wallets/${id}`, data),
  delete: (id) => apiClient.delete(`/wallets/${id}`),
  // ... other methods
};
```

### **3. Use in Components**
```javascript
// WalletList.jsx
import { walletService } from '../api/walletService';

const WalletList = () => {
  const [wallets, setWallets] = useState([]);
  const [loading, setLoading] = useState(false);
  
  useEffect(() => {
    loadWallets();
  }, []);
  
  const loadWallets = async () => {
    setLoading(true);
    try {
      const { data } = await walletService.getAll();
      setWallets(data.wallets);
    } catch (error) {
      handleApiError(error);
    } finally {
      setLoading(false);
    }
  };
  
  return (
    // JSX rendering
  );
};
```

---

## 🎨 **UI/UX RECOMMENDATIONS**

### **Transaction with Conversion Display:**
```javascript
const TransactionItem = ({ transaction }) => {
  const isConverted = transaction.originalAmount && transaction.originalCurrency;
  
  return (
    <div className="transaction-item">
      <div className="amount">
        {formatMoney(transaction.amount)} {transaction.wallet.currencyCode}
      </div>
      
      {isConverted && (
        <div className="conversion-badge">
          <Icon name="exchange" />
          Đã chuyển đổi từ {transaction.originalAmount} {transaction.originalCurrency}
          <Tooltip>
            Tỷ giá: 1 {transaction.originalCurrency} = {transaction.exchangeRate}
            <br/>
            Ngày gộp ví: {formatDate(transaction.mergeDate)}
          </Tooltip>
        </div>
      )}
    </div>
  );
};
```

### **Wallet Card with Role Badge:**
```javascript
const WalletCard = ({ wallet }) => {
  return (
    <div className="wallet-card">
      <div className="header">
        <h3>{wallet.walletName}</h3>
        {wallet.myRole === "MEMBER" && (
          <Badge color="blue">Được chia sẻ</Badge>
        )}
        {wallet.isDefault && (
          <Badge color="green">Mặc định</Badge>
        )}
      </div>
      
      <div className="balance">
        {formatMoney(wallet.balance)} {wallet.currencyCode}
      </div>
      
      {wallet.totalMembers > 1 && (
        <div className="members">
          <Icon name="users" />
          {wallet.totalMembers} thành viên
        </div>
      )}
      
      <div className="actions">
        {wallet.myRole === "OWNER" && (
          <>
            <Button onClick={handleEdit}>Sửa</Button>
            <Button onClick={handleDelete}>Xóa</Button>
            <Button onClick={handleShare}>Chia sẻ</Button>
          </>
        )}
        {wallet.myRole === "MEMBER" && (
          <Button onClick={handleLeave}>Rời khỏi</Button>
        )}
        <Button onClick={handleViewTransactions}>Xem giao dịch</Button>
      </div>
    </div>
  );
};
```

---

## 🧪 **TESTING CHECKLIST**

### **Authentication:**
- [ ] Đăng ký với email hợp lệ
- [ ] Đăng ký với email đã tồn tại
- [ ] Verify với code đúng/sai
- [ ] Login thành công
- [ ] Login với wrong password
- [ ] Google OAuth login
- [ ] Refresh token khi expired
- [ ] Logout và clear tokens

### **Wallet:**
- [ ] Tạo ví với currency khác nhau
- [ ] Set default wallet
- [ ] Update wallet name (OWNER)
- [ ] Update balance khi chưa có transaction
- [ ] Không cho update balance khi đã có transaction
- [ ] Delete wallet
- [ ] Delete default wallet → auto set new default
- [ ] MEMBER không thể edit/delete

### **Shared Wallet:**
- [ ] OWNER share ví
- [ ] Share với email không tồn tại → error
- [ ] Share với chính mình → error
- [ ] MEMBER view members
- [ ] OWNER remove member
- [ ] MEMBER leave wallet
- [ ] OWNER không thể leave → error

### **Transaction:**
- [ ] OWNER tạo expense/income
- [ ] MEMBER tạo expense/income
- [ ] Expense vượt số dư → error
- [ ] Wallet balance update đúng

### **Money Transfer:**
- [ ] Transfer giữa ví cùng currency
- [ ] Transfer khác currency → error
- [ ] Transfer vượt số dư → error
- [ ] 2 transactions được tạo
- [ ] Balance cả 2 ví update đúng

### **Wallet Merge:**
- [ ] Get merge candidates
- [ ] Candidates không bao gồm shared wallet
- [ ] Preview merge cùng currency
- [ ] Preview merge khác currency với conversion
- [ ] Merge thành công
- [ ] Transactions được convert đúng
- [ ] Source wallet bị xóa
- [ ] Transaction hiển thị conversion info

---

## 🎯 **CURRENCY CONVERSION DETAILS**

### **Supported Currencies:**
- VND (Vietnamese Dong)
- USD (US Dollar)
- EUR (Euro)
- JPY (Japanese Yen)
- GBP (British Pound)
- CNY (Chinese Yuan)

### **Exchange Rates (Fixed - v1.0):**
```javascript
const EXCHANGE_RATES = {
  "USD_VND": 24350,  // 1 USD = 24,350 VND
  "EUR_VND": 26315,  // 1 EUR = 26,315 VND
  "JPY_VND": 158,    // 1 JPY = 158 VND
  "GBP_VND": 31250,  // 1 GBP = 31,250 VND
  "CNY_VND": 3333,   // 1 CNY = 3,333 VND
};
```

### **Frontend Currency Selector:**
```javascript
const CurrencySelector = ({ sourceWallet, targetWallet, onChange }) => {
  const currencies = [
    {
      code: sourceWallet.currencyCode,
      label: `Giữ ${sourceWallet.currencyCode}`,
      description: `Chuyển ${targetWallet.currencyCode} → ${sourceWallet.currencyCode}`,
      rate: getExchangeRate(targetWallet.currencyCode, sourceWallet.currencyCode)
    },
    {
      code: targetWallet.currencyCode,
      label: `Giữ ${targetWallet.currencyCode}`,
      description: `Chuyển ${sourceWallet.currencyCode} → ${targetWallet.currencyCode}`,
      rate: getExchangeRate(sourceWallet.currencyCode, targetWallet.currencyCode)
    }
  ];
  
  return (
    <RadioGroup onChange={onChange}>
      {currencies.map(curr => (
        <Radio key={curr.code} value={curr.code}>
          {curr.label}
          <div className="description">
            {curr.description}
            <br/>
            Tỷ giá: {curr.rate}
          </div>
        </Radio>
      ))}
    </RadioGroup>
  );
};
```

---

## 📞 **SUPPORT & TROUBLESHOOTING**

### **Common Issues:**

**1. CORS Error:**
```
Access to XMLHttpRequest blocked by CORS policy
```
**Fix:** Backend đã config CORS cho `http://localhost:3000`. Đảm bảo frontend chạy đúng port.

---

**2. Token Expired:**
```
401 Unauthorized
```
**Fix:** Dùng interceptor để auto refresh token (xem phần Base Configuration)

---

**3. Port Already in Use:**
```
Port 8080 was already in use
```
**Fix:** Kill process hoặc đổi port trong `application.properties`

---

**4. Transaction Balance Mismatch:**
**Fix:** Đảm bảo luôn refresh wallet list sau mỗi transaction/transfer/merge

---

## ✅ **FINAL CHECKLIST**

Trước khi integrate, đảm bảo:

- [ ] Backend đang chạy ở `http://localhost:8080`
- [ ] Database migrations đã chạy (bao gồm `database_migration_currency_conversion.sql`)
- [ ] API client đã setup với interceptors
- [ ] Error handling đã implement đầy đủ
- [ ] Loading states cho mọi async operations
- [ ] Validation ở frontend trước khi call API
- [ ] Token refresh logic đã hoạt động
- [ ] Protected routes đã setup
- [ ] Hiển thị role badges cho shared wallets
- [ ] Hiển thị conversion info cho converted transactions

---

## 🚀 **DEPLOYMENT NOTES**

### **Production Configuration:**

```javascript
// .env.production
REACT_APP_API_URL=https://api.yourapp.com

// API client
const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';
```

### **Backend Production:**
- Update CORS allowed origins
- Use HTTPS
- Implement real-time exchange rate API
- Add rate limiting
- Add logging/monitoring

---

## 📖 **ADDITIONAL RESOURCES**

- **Postman Collection:** Import all endpoints for testing
- **API Changelog:** Track API version changes
- **Frontend Examples:** Check `/examples` folder (TODO)

---

**🎉 HẾT - Tất cả thông tin cần thiết để integrate Frontend với Backend!**

**Questions? Contact Backend Team!**

