import React from "react";
import { formatMoneyInput, getMoneyValue } from "../../../utils/formatMoneyInput";
import { formatMoney } from "../../../utils/formatMoney";

export default function TopupTab({
  wallet,
  incomeCategories = [],
  topupAmount,
  setTopupAmount,
  topupNote,
  setTopupNote,
  topupCategoryId,
  setTopupCategoryId,
  onSubmitTopup,
}) {

  const currentBalance = Number(wallet?.balance || 0);
  const walletCurrency = wallet?.currency || "VND";

  return (
    <div className="wallets-section">
      <div className="wallets-section__header">
        <h3>Nạp tiền vào ví</h3>
        <span>Nạp thêm số dư cho ví hiện tại.</span>
      </div>
      <form className="wallet-form" onSubmit={onSubmitTopup} autoComplete="off">
        <div className="wallet-form__row">
          <label>
            Danh mục <span style={{ color: "#ef4444" }}>*</span>
            <select
              value={topupCategoryId}
              onChange={(e) => setTopupCategoryId(e.target.value)}
              required
            >
              <option value="">Chọn danh mục</option>
              {incomeCategories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Số tiền nạp
            <input
              type="text"
              value={formatMoneyInput(topupAmount)}
              onChange={(e) => {
                const parsed = getMoneyValue(e.target.value);
                setTopupAmount(parsed ? String(parsed) : "");
              }}
              placeholder="Nhập số tiền..."
              inputMode="numeric"
            />
            <div style={{ 
              fontSize: "0.875rem", 
              color: "#6b7280",
              marginTop: "4px"
            }}>
              Số dư hiện tại:{" "}
              <strong style={{ color: "#111827" }}>
                {formatMoney(currentBalance, walletCurrency)}
              </strong>
            </div>
          </label>
        </div>

        <div className="wallet-form__row">
          <label className="wallet-form__full">
            Ghi chú
            <textarea
              rows={2}
              value={topupNote}
              onChange={(e) => setTopupNote(e.target.value)}
              placeholder="Nhập ghi chú (tùy chọn)"
            />
          </label>
        </div>

        {/* Hiển thị lỗi nếu số tiền hoặc danh mục không hợp lệ */}
        {topupAmount && (
          <div className="wallet-form__row">
            {!topupCategoryId && (
              <div style={{ color: "#ef4444", fontSize: "0.875rem", marginTop: "-10px", marginBottom: "10px" }}>
                Vui lòng chọn danh mục.
              </div>
            )}
            {topupCategoryId && (!topupAmount || Number(topupAmount) <= 0) && (
              <div style={{ color: "#ef4444", fontSize: "0.875rem", marginTop: "-10px", marginBottom: "10px" }}>
                Số tiền không hợp lệ.
              </div>
            )}
          </div>
        )}

        <div className="wallet-form__footer wallet-form__footer--right">
          <button
            type="submit"
            className="wallets-btn wallets-btn--primary"
            disabled={!topupAmount || !topupCategoryId || Number(topupAmount) <= 0}
          >
            <span style={{ marginRight: "6px" }}>✔</span>
            Xác nhận nạp
          </button>
        </div>
      </form>
    </div>
  );
}

