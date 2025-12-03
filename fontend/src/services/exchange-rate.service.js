/**
 * Exchange Rate Service - Lấy tỉ giá VND/USD từ API
 */

const EXCHANGE_API_URL = "https://api.exchangerate-api.com/v4/latest/USD";
const EXCHANGE_HISTORICAL_API = "https://api.exchangerate-api.com/v4/history/USD";
const FALLBACK_RATE = 24500; // Tỉ giá fallback

/**
 * Lấy tỉ giá VND/USD từ API
 */
export async function getExchangeRate() {
  // Kiểm tra cache trước
  const cached = getCachedRate();
  if (cached) {
    return cached;
  }

  try {
    // Thử dùng exchangerate-api.com (miễn phí, không cần API key)
    const response = await fetch(EXCHANGE_API_URL, {
      method: "GET",
      headers: {
        "Accept": "application/json",
      },
    });

    if (!response.ok) {
      throw new Error(`Exchange rate API error: ${response.status}`);
    }

    const data = await response.json();
    
    // Lấy tỉ giá VND từ API (1 USD = ? VND)
    const vndRate = data.rates?.VND || FALLBACK_RATE;
    
    // Tính toán
    const vndToUsd = Math.round(vndRate); // 1 USD = ? VND
    const usdToVnd = 1 / vndRate; // 1 VND = ? USD
    
    // Lấy giá trị cũ để tính thay đổi
    const oldCached = localStorage.getItem("exchange_rate_previous");
    let change = 0;
    let changePercent = 0;
    
    if (oldCached) {
      try {
        const oldData = JSON.parse(oldCached);
        const oldRate = oldData.vndToUsd || vndToUsd;
        change = vndToUsd - oldRate;
        changePercent = (change / oldRate) * 100;
      } catch (e) {
        // Ignore parse error
      }
    }
    
    // Lưu giá trị hiện tại làm giá trị cũ cho lần sau
    localStorage.setItem("exchange_rate_previous", JSON.stringify({
      vndToUsd,
      lastUpdate: new Date().toISOString(),
    }));

    const rateData = {
      vndToUsd,
      usdToVnd,
      change,
      changePercent,
      lastUpdate: new Date().toISOString(),
    };

    // Cache kết quả
    cacheRate(rateData);
    
    return rateData;
  } catch (error) {
    console.error("Error fetching exchange rate:", error);
    
    // Fallback: dùng tỉ giá cố định hoặc cache
    if (cached) {
      return cached;
    }
    
    return {
      vndToUsd: FALLBACK_RATE,
      usdToVnd: 1 / FALLBACK_RATE,
      change: 0,
      changePercent: 0,
      lastUpdate: new Date().toISOString(),
    };
  }
}

/**
 * Lấy tỉ giá từ localStorage (nếu đã lưu)
 */
export function getCachedRate() {
  try {
    const cached = localStorage.getItem("exchange_rate_cache");
    if (cached) {
      const data = JSON.parse(cached);
      const cacheTime = new Date(data.lastUpdate);
      const now = new Date();
      
      // Nếu cache còn hiệu lực (dưới 5 phút)
      if (now - cacheTime < 5 * 60 * 1000) {
        return data;
      }
    }
  } catch (error) {
    console.error("Error reading cached rate:", error);
  }
  return null;
}

/**
 * Lưu tỉ giá vào localStorage
 */
export function cacheRate(rateData) {
  try {
    localStorage.setItem("exchange_rate_cache", JSON.stringify(rateData));
    
    // Lưu vào lịch sử
    saveRateToHistory(rateData.vndToUsd);
  } catch (error) {
    console.error("Error caching rate:", error);
  }
}

/**
 * Lưu tỉ giá vào lịch sử (tối đa 30 ngày)
 */
export function saveRateToHistory(vndToUsd) {
  try {
    let history = [];
    try {
      const stored = localStorage.getItem("exchange_rate_history");
      if (stored) {
        history = JSON.parse(stored);
      }
    } catch (e) {
      console.warn("Error reading history:", e);
    }
    
    const now = new Date();
    const today = now.toISOString().split("T")[0]; // YYYY-MM-DD
    const currentTime = now.toISOString();
    
    // Thêm điểm dữ liệu mới với timestamp
    history.push({
      date: currentTime,
      value: vndToUsd,
      day: today, // Để nhóm theo ngày
    });
    
    // Loại bỏ trùng lặp và sắp xếp
    const unique = history.filter((item, index, self) => 
      index === self.findIndex((t) => t.date === item.date)
    );
    unique.sort((a, b) => new Date(a.date) - new Date(b.date));
    
    // Giữ tối đa 30 ngày
    const maxDays = 30;
    if (unique.length > maxDays * 10) { // Giữ nhiều điểm trong ngày
      unique.splice(0, unique.length - maxDays * 10);
    }
    
    localStorage.setItem("exchange_rate_history", JSON.stringify(unique));
  } catch (error) {
    console.error("Error saving rate history:", error);
  }
}

/**
 * Lấy lịch sử tỉ giá từ API (7 ngày gần nhất) - sử dụng exchangerate.host
 */
export async function fetchRateHistory() {
  try {
    // Lấy dữ liệu từ localStorage trước (dữ liệu thật đã lưu)
    const cached = getRateHistory();
    if (cached && cached.length >= 7) {
      return cached; // Đã có đủ dữ liệu
    }
    
    const history = [];
    const today = new Date();
    
    // Thử lấy từ exchangerate.host (miễn phí, có historical)
    try {
      // Lấy dữ liệu 7 ngày gần nhất
      for (let i = 6; i >= 0; i--) {
        const date = new Date(today);
        date.setDate(date.getDate() - i);
        const dateStr = date.toISOString().split("T")[0]; // YYYY-MM-DD
        
        // Thử exchangerate.host
        try {
          const response = await fetch(
            `https://api.exchangerate.host/${dateStr}?base=USD&symbols=VND`,
            {
              method: "GET",
              headers: {
                "Accept": "application/json",
              },
            }
          );
          
          if (response.ok) {
            const data = await response.json();
            const vndRate = data.rates?.VND || null;
            
            if (vndRate) {
              history.push({
                date: date.toISOString(),
                value: Math.round(vndRate),
              });
              continue;
            }
          }
        } catch (e) {
          console.warn(`Failed to fetch from exchangerate.host for ${dateStr}:`, e);
        }
        
        // Nếu không lấy được, dùng dữ liệu từ localStorage
        const cachedForDate = getCachedHistoryForDate(dateStr);
        if (cachedForDate) {
          history.push(cachedForDate);
        }
      }
      
      // Nếu vẫn chưa đủ, lấy từ localStorage và fill gaps
      if (history.length < 7) {
        const allCached = getAllCachedHistory();
        const last7Days = [];
        const today = new Date();
        
        for (let i = 6; i >= 0; i--) {
          const date = new Date(today);
          date.setDate(date.getDate() - i);
          const dateStr = date.toISOString().split("T")[0];
          
          // Tìm trong cached
          const found = allCached.find((d) => d.day === dateStr || d.date.startsWith(dateStr));
          if (found) {
            last7Days.push({
              date: found.date,
              value: found.value,
            });
          } else if (history.length > 0) {
            // Dùng giá trị gần nhất
            const lastValue = history[history.length - 1].value;
            last7Days.push({
              date: date.toISOString(),
              value: lastValue,
            });
          } else if (allCached.length > 0) {
            // Dùng giá trị từ cache
            const lastCached = allCached[allCached.length - 1];
            last7Days.push({
              date: date.toISOString(),
              value: lastCached.value,
            });
          } else {
            // Fallback cuối cùng
            last7Days.push({
              date: date.toISOString(),
              value: FALLBACK_RATE,
            });
          }
        }
        return last7Days;
      }
      
      return history;
    } catch (error) {
      console.error("Error fetching rate history from API:", error);
      return getRateHistory(); // Fallback về localStorage
    }
  } catch (error) {
    console.error("Error fetching rate history:", error);
    return getRateHistory();
  }
}

/**
 * Lấy tất cả lịch sử từ localStorage
 */
function getAllCachedHistory() {
  try {
    const history = localStorage.getItem("exchange_rate_history");
    if (history) {
      const data = JSON.parse(history);
      return data.map((item) => ({
        ...item,
        day: item.day || item.date.split("T")[0],
      }));
    }
  } catch (error) {
    console.error("Error reading all cached history:", error);
  }
  return [];
}

/**
 * Lấy lịch sử tỉ giá từ localStorage (7 ngày gần nhất, mỗi ngày 1 điểm)
 */
export function getRateHistory() {
  try {
    const allHistory = getAllCachedHistory();
    
    if (allHistory.length === 0) {
      return [];
    }
    
    // Sắp xếp theo ngày
    allHistory.sort((a, b) => new Date(a.date) - new Date(b.date));
    
    // Nhóm theo ngày và lấy giá trị cuối cùng của mỗi ngày
    const byDay = {};
    allHistory.forEach((item) => {
      const day = item.day || item.date.split("T")[0];
      if (!byDay[day] || new Date(item.date) > new Date(byDay[day].date)) {
        byDay[day] = item;
      }
    });
    
    // Chuyển thành array và sắp xếp
    const dailyData = Object.values(byDay).sort((a, b) => new Date(a.date) - new Date(b.date));
    
    // Lấy 7 ngày gần nhất
    const last7Days = dailyData.slice(-7);
    
    // Đảm bảo có đủ 7 ngày (fill gaps nếu cần)
    if (last7Days.length < 7) {
      const today = new Date();
      const filled = [];
      const lastValue = last7Days.length > 0 ? last7Days[last7Days.length - 1].value : FALLBACK_RATE;
      
      for (let i = 6; i >= 0; i--) {
        const date = new Date(today);
        date.setDate(date.getDate() - i);
        const dateStr = date.toISOString().split("T")[0];
        
        const existing = last7Days.find((d) => {
          const dDay = d.day || d.date.split("T")[0];
          return dDay === dateStr;
        });
        
        if (existing) {
          filled.push({
            date: existing.date,
            value: existing.value,
          });
        } else {
          // Dùng giá trị gần nhất
          filled.push({
            date: date.toISOString(),
            value: lastValue,
          });
        }
      }
      return filled;
    }
    
    // Format lại để đảm bảo có date và value
    return last7Days.map((item) => ({
      date: item.date,
      value: item.value,
    }));
  } catch (error) {
    console.error("Error reading rate history:", error);
    return [];
  }
}

/**
 * Lấy tỉ giá từ localStorage cho một ngày cụ thể
 */
function getCachedHistoryForDate(dateStr) {
  try {
    const allHistory = getAllCachedHistory();
    const dayData = allHistory.filter((d) => {
      const dDay = d.day || d.date.split("T")[0];
      return dDay === dateStr;
    });
    
    if (dayData.length > 0) {
      // Lấy giá trị cuối cùng trong ngày
      dayData.sort((a, b) => new Date(b.date) - new Date(a.date));
      return {
        date: dayData[0].date,
        value: dayData[0].value,
      };
    }
  } catch (error) {
    console.error("Error reading cached history:", error);
  }
  return null;
}

