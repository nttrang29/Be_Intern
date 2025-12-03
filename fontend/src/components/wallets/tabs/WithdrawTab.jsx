import React from "react";
import { formatMoneyInput, getMoneyValue } from "../../../utils/formatMoneyInput";
import { formatMoney } from "../../../utils/formatMoney";

export default function WithdrawTab({
  wallet,
  expenseCategories = [],
  withdrawAmount,
  setWithdrawAmount,
  withdrawNote,
  setWithdrawNote,
  withdrawCategoryId,
  setWithdrawCategoryId,
  onSubmitWithdraw,
}) {

  const currentBalance = Number(wallet?.balance || 0);
  const walletCurrency = wallet?.currency || "VND";

  return (
    <div className="wallets-section">
      <div className="wallets-section__header">
        <h3>Rút tiền từ ví</h3>
        <span>Rút tiền và chọn danh mục phù hợp.</span>
      </div>
      <form
        className="wallet-form"
        onSubmit={onSubmitWithdraw}
        autoComplete="off"
      >
        <div className="wallet-form__row">
          <label>
            Danh mục <span style={{ color: "#ef4444" }}>*</span>
            <select
              value={withdrawCategoryId}
              onChange={(e) => setWithdrawCategoryId(e.target.value)}
              required
            >
              <option value="">Chọn danh mục</option>
              {expenseCategories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Số tiền rút
            <input
              type="text"
              value={formatMoneyInput(withdrawAmount)}
              onChange={(e) => {
                const parsed = getMoneyValue(e.target.value);
                setWithdrawAmount(parsed ? String(parsed) : "");
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
              value={withdrawNote}
              onChange={(e) => setWithdrawNote(e.target.value)}
              placeholder="Nhập ghi chú (tùy chọn)"
            />
          </label>
        </div>

        {/* Hiển thị lỗi nếu số tiền không hợp lệ */}
        {withdrawAmount && (
          <div className="wallet-form__row">
            {!withdrawCategoryId && (
              <div style={{ color: "#ef4444", fontSize: "0.875rem", marginTop: "-10px", marginBottom: "10px" }}>
                Vui lòng chọn danh mục.
              </div>
            )}
            {withdrawCategoryId && Number(withdrawAmount) > currentBalance && (
              <div style={{ color: "#ef4444", fontSize: "0.875rem", marginTop: "-10px", marginBottom: "10px" }}>
                Số tiền không hợp lệ hoặc vượt quá số dư.
              </div>
            )}
          </div>
        )}

        <div className="wallet-form__footer wallet-form__footer--right">
          <button
            type="submit"
            className="wallets-btn wallets-btn--primary"
            disabled={!withdrawAmount || !withdrawCategoryId || Number(withdrawAmount) > currentBalance}
          >
            <span style={{ marginRight: "6px" }}>✔</span>
            Xác nhận rút
          </button>
        </div>
      </form>
    </div>
  );
}

