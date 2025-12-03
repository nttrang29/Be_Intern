import React, { useState, useEffect } from "react";

export default function ConvertTab({
  wallet,
  allWallets = [],
  onConvertToGroup,
  onChangeSelectedWallet,
}) {
  const isDefault = !!wallet.isDefault;
  const isShared = !!wallet.isShared;

  const personalWallets = (allWallets || []).filter((w) => !w.isShared);
  const candidateDefaults = personalWallets.filter((w) => w.id !== wallet.id);
  const hasCandidate = candidateDefaults.length > 0;

  const [defaultMode, setDefaultMode] = useState(
    hasCandidate ? "chooseOther" : "noDefault"
  );
  const [newDefaultId, setNewDefaultId] = useState(
    hasCandidate ? String(candidateDefaults[0].id) : ""
  );

  useEffect(() => {
    const newCandidates = (allWallets || []).filter(
      (w) => !w.isShared && w.id !== wallet.id
    );
    const hasCandidateNow = newCandidates.length > 0;

    setDefaultMode(hasCandidateNow ? "chooseOther" : "noDefault");
    setNewDefaultId(hasCandidateNow ? String(newCandidates[0].id) : "");
  }, [wallet.id, allWallets]);

  const handleSubmit = (e) => {
    e.preventDefault();

    let options = null;

    if (isDefault && !isShared) {
      if (hasCandidate && defaultMode === "chooseOther" && newDefaultId) {
        options = {
          newDefaultWalletId: Number(newDefaultId),
          noDefault: false,
        };
      } else {
        options = {
          newDefaultWalletId: null,
          noDefault: true,
        };
      }
    }

    onConvertToGroup?.(e, options || null);

    if (onChangeSelectedWallet) {
      onChangeSelectedWallet(null);
    }
  };

  const isSubmitDisabled =
    wallet.isShared ||
    (isDefault &&
      !isShared &&
      defaultMode === "chooseOther" &&
      hasCandidate &&
      !newDefaultId);

  return (
    <div className="wallets-section">
      <div className="wallets-section__header">
        <h3>Chuyển thành ví nhóm</h3>
        <span>
          Sau khi chuyển, ví này sẽ trở thành ví nhóm. Bạn có thể thêm thành
          viên ở phần chia sẻ.
        </span>
      </div>

      <form className="wallet-form" onSubmit={handleSubmit}>
        <div className="wallet-form__row">
          <label className="wallet-form__full">
            <span className="wallet-detail-item__label">Tóm tắt ví</span>
            <div className="wallet-detail-item" style={{ marginTop: 4 }}>
              <div className="wallet-detail-item__value">
                <strong>Tên ví:</strong> {wallet.name}
              </div>
              <div className="wallet-detail-item__value">
                <strong>Trạng thái:</strong>{" "}
                {wallet.isShared ? "Đã là ví nhóm" : "Hiện là ví cá nhân"}
              </div>
              {isDefault && !wallet.isShared && (
                <div
                  className="wallet-detail-item__value"
                  style={{ marginTop: 4 }}
                >
                  <strong>Ghi chú:</strong> Ví này đang là ví mặc định.
                </div>
              )}
            </div>
          </label>
        </div>

        {isDefault && !wallet.isShared && (
          <>
            <div className="wallet-merge__section-block wallet-merge__section-block--warning">
              <div className="wallet-merge__section-title">
                Bạn đang chuyển một ví mặc định sang ví nhóm
              </div>
              <ul className="wallet-merge__list">
                <li>
                  <strong>{wallet.name}</strong> hiện đang là ví mặc định của hệ
                  thống.
                </li>
                <li>
                  Ví nhóm không được phép đặt làm ví mặc định, vì vậy cần chọn
                  cách xử lý ví mặc định hiện tại.
                </li>
              </ul>
            </div>

            <div className="wallet-merge__section-block">
              <div className="wallet-merge__section-title">
                Chọn cách xử lý ví mặc định
              </div>

              {hasCandidate ? (
                <div className="wallet-merge__options">
                  <label className="wallet-merge__option">
                    <input
                      type="radio"
                      name="defaultBehavior"
                      value="chooseOther"
                      checked={defaultMode === "chooseOther"}
                      onChange={() => setDefaultMode("chooseOther")}
                    />
                    <div>
                      <div className="wallet-merge__option-title">
                        Chọn một ví cá nhân khác làm ví mặc định mới
                      </div>
                      <div className="wallet-merge__option-desc">
                        Sau khi chuyển sang ví nhóm, ví được chọn dưới đây sẽ trở
                        thành ví mặc định.
                      </div>
                      <div style={{ marginTop: 6 }}>
                        <select
                          value={newDefaultId}
                          disabled={defaultMode !== "chooseOther"}
                          onChange={(e) => setNewDefaultId(e.target.value)}
                        >
                          {candidateDefaults.map((w) => (
                            <option key={w.id} value={w.id}>
                              {w.name || "Ví cá nhân khác"}
                            </option>
                          ))}
                        </select>
                      </div>
                    </div>
                  </label>

                  <label className="wallet-merge__option">
                    <input
                      type="radio"
                      name="defaultBehavior"
                      value="noDefault"
                      checked={defaultMode === "noDefault"}
                      onChange={() => setDefaultMode("noDefault")}
                    />
                    <div>
                      <div className="wallet-merge__option-title">
                        Tạm thời không có ví mặc định
                      </div>
                      <div className="wallet-merge__option-desc">
                        Hệ thống sẽ tạm thời không có ví mặc định. Bạn có thể
                        đặt lại ví mặc định sau trong phần quản lý ví.
                      </div>
                    </div>
                  </label>
                </div>
              ) : (
                <div className="wallet-merge__section-block">
                  <p className="wallet-merge__hint">
                    Hiện tại bạn không có ví cá nhân nào khác. Sau khi chuyển
                    ví này thành ví nhóm, hệ thống sẽ tạm thời không có ví mặc
                    định. Bạn có thể tạo ví cá nhân mới và đặt làm mặc định sau.
                  </p>
                </div>
              )}
            </div>
          </>
        )}

        <div className="wallet-form__footer wallet-form__footer--right">
          <button
            type="submit"
            className="wallets-btn wallets-btn--primary"
            disabled={isSubmitDisabled}
          >
            {wallet.isShared ? "Đã là ví nhóm" : "Chuyển sang ví nhóm"}
          </button>
        </div>
      </form>
    </div>
  );
}

