import React, { useEffect, useState } from "react";
import Modal from "./Modal";

/**
 * Modal báo lỗi (ví dụ: tài khoản đã tồn tại)
 * - Tự đóng sau {seconds} giây
 * - Không điều hướng, ở lại trang đăng ký
 */
export default function AccountExistsModal({
  open,
  onClose,
  title,
  message,
  seconds,
}) {
  const [remain, setRemain] = useState(seconds);

  useEffect(() => {
    if (!open) return;
    setRemain(seconds);
    const t = setInterval(() => {
      setRemain((s) => {
        if (s <= 1) {
          clearInterval(t);
          onClose?.();
          return 0;
        }
        return s - 1;
      });
    }, 1000);
    return () => clearInterval(t);
  }, [open, seconds, onClose]);

  return (
    <Modal open={open} onClose={() => {}} width={480}>
      {/* Header */}
      <div className="modal__header">
        <strong>{title}</strong>
      </div>

      {/* Body */}
      <div className="modal__body text-center">
        {/* Icon lỗi (đỏ) */}
        <div className="modal__icon-error">!</div>

        <h5 className="mb-2">{message}</h5>
        {/* ❌ Bỏ dòng “Tự đóng sau ...” */}
      </div>
    </Modal>
  );
}
