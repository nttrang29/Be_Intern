/**
 * Utility functions cho wallet components
 */

// Helper function để tính tỷ giá (dùng chung cho tất cả components)
export function getRate(from, to) {
  if (!from || !to || from === to) return 1;
  
  // Tỷ giá cố định (theo ExchangeRateServiceImpl)
  // rates[currency] = tỷ giá 1 VND = ? currency
  const ratesToVND = {
    VND: 1,
    USD: 0.000041, // 1 VND = 0.000041 USD
    EUR: 0.000038,
    JPY: 0.0063,
    GBP: 0.000032,
    CNY: 0.00030,
  };
  
  // Tỷ giá ngược lại: 1 currency = ? VND (để tránh phép chia)
  const ratesFromVND = {
    VND: 1,
    USD: 24390.243902439024, // 1 USD = 24390.243902439024 VND (1/0.000041)
    EUR: 26315.78947368421, // 1 EUR = 26315.78947368421 VND (1/0.000038)
    JPY: 158.73015873015873, // 1 JPY = 158.73015873015873 VND (1/0.0063)
    GBP: 31250, // 1 GBP = 31250 VND (1/0.000032)
    CNY: 3333.3333333333335, // 1 CNY = 3333.3333333333335 VND (1/0.00030)
  };
  
  if (!ratesToVND[from] || !ratesToVND[to]) return 1;
  
  // Nếu from là VND, tỷ giá đơn giản là ratesToVND[to]
  if (from === "VND") {
    return ratesToVND[to];
  }
  // Nếu to là VND, tỷ giá là ratesFromVND[from] (tránh phép chia)
  if (to === "VND") {
    return ratesFromVND[from];
  }
  // Tính tỷ giá: from → VND → to
  // 1 from = ratesFromVND[from] VND
  // ratesFromVND[from] VND = ratesFromVND[from] * ratesToVND[to] to
  // VD: USD → EUR: 1 USD = 24390.243902439024 VND = 24390.243902439024 * 0.000038 EUR
  // Tỷ giá from → to = ratesFromVND[from] * ratesToVND[to]
  const rate = ratesFromVND[from] * ratesToVND[to];
  // Sử dụng toFixed(8) rồi parseFloat để giảm sai số tích lũy
  return parseFloat(rate.toFixed(8));
}

// Format số dư sau khi chuyển đổi với độ chính xác cao (8 chữ số thập phân)
export function formatConvertedBalance(amount = 0, currency = "VND") {
  const numAmount = Number(amount) || 0;
  if (currency === "VND") {
    // VND: hiển thị với 8 chữ số thập phân để khớp với tỷ giá (không làm tròn về số nguyên)
    // Kiểm tra xem có phần thập phân không
    const hasDecimal = numAmount % 1 !== 0;
    if (hasDecimal) {
      const formatted = numAmount.toLocaleString("vi-VN", { 
        minimumFractionDigits: 0, 
        maximumFractionDigits: 8 
      });
      return `${formatted} VND`;
    }
    // Nếu là số nguyên, hiển thị bình thường
    return `${numAmount.toLocaleString("vi-VN")} VND`;
  }
  if (currency === "USD") {
    // USD: hiển thị với 8 chữ số thập phân để khớp với tỷ giá
    const formatted = numAmount.toLocaleString("en-US", { 
      minimumFractionDigits: 0, 
      maximumFractionDigits: 8 
    });
    return `$${formatted}`;
  }
  // Các currency khác
  const formatted = numAmount.toLocaleString("vi-VN", { 
    minimumFractionDigits: 0, 
    maximumFractionDigits: 8 
  });
  return `${formatted} ${currency}`;
}

// Format tỷ giá với độ chính xác cao
export function formatExchangeRate(rate = 0, toCurrency = "VND") {
  const numRate = Number(rate) || 0;
  if (toCurrency === "USD") {
    return numRate.toLocaleString("en-US", { 
      minimumFractionDigits: 0, 
      maximumFractionDigits: 8 
    });
  }
  return numRate.toLocaleString("vi-VN", { 
    minimumFractionDigits: 0, 
    maximumFractionDigits: 8 
  });
}

