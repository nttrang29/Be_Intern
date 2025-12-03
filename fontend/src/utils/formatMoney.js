/**
 * Utility functions để format số tiền hiển thị
 * Format số tiền với độ chính xác cao (tối đa 8 chữ số thập phân)
 * Để hiển thị chính xác số tiền nhỏ khi chuyển đổi tiền tệ
 */

/**
 * Format số tiền với currency
 * @param {number|string} amount - Số tiền cần format
 * @param {string} currency - Loại tiền tệ (VND, USD, EUR, etc.)
 * @returns {string} - Chuỗi đã format (ví dụ: "1.000.000 VND" hoặc "$100.00")
 */
export function formatMoney(amount = 0, currency = "VND") {
  const numAmount = Number(amount) || 0;
  
  // Custom format cho USD: hiển thị $ ở trước
  // Sử dụng tối đa 8 chữ số thập phân để hiển thị chính xác số tiền nhỏ
  if (currency === "USD") {
    // Nếu số tiền rất nhỏ (< 0.01), hiển thị nhiều chữ số thập phân hơn
    if (Math.abs(numAmount) < 0.01 && numAmount !== 0) {
      const formatted = numAmount.toLocaleString("en-US", { 
        minimumFractionDigits: 2, 
        maximumFractionDigits: 8 
      });
      return `$${formatted}`;
    }
    const formatted = numAmount % 1 === 0 
      ? numAmount.toLocaleString("en-US")
      : numAmount.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 8 });
    return `$${formatted}`;
  }
  
  // Format cho VND và các currency khác
  try {
    if (currency === "VND") {
      // VND: hiển thị số thập phân nếu có (khi chuyển đổi từ currency khác)
      const hasDecimal = numAmount % 1 !== 0;
      if (hasDecimal) {
        const formatted = numAmount.toLocaleString("vi-VN", { 
          minimumFractionDigits: 0, 
          maximumFractionDigits: 8 
        });
        return `${formatted} VND`;
      }
      return `${numAmount.toLocaleString("vi-VN")} VND`;
    }
    // Với các currency khác, cũng hiển thị tối đa 8 chữ số thập phân để chính xác
    if (Math.abs(numAmount) < 0.01 && numAmount !== 0) {
      return `${numAmount.toLocaleString("vi-VN", { minimumFractionDigits: 2, maximumFractionDigits: 8 })} ${currency}`;
    }
    return `${numAmount.toLocaleString("vi-VN", { minimumFractionDigits: 2, maximumFractionDigits: 8 })} ${currency}`;
  } catch {
    return `${numAmount.toLocaleString("vi-VN")} ${currency}`;
  }
}

