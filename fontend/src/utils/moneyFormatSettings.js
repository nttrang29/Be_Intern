// src/utils/moneyFormatSettings.js

export const MONEY_FORMATS = [
  {
    key: "space",
    label: "1 234 567 (cách nhau bằng khoảng trắng)",
    group: " ",
    decimal: ",",
    thousand: " ",
  },
  {
    key: "dot",
    label: "1.234.567 (dấu chấm)",
    group: ".",
    decimal: ",",
    thousand: ".",
  },
  {
    key: "comma",
    label: "1,234,567 (dấu phẩy)",
    group: ",",
    decimal: ".",
    thousand: ",",
  },
];

export function getMoneyFormatSettings() {
  const formatKey = localStorage.getItem("moneyFormat") || "space";
  const decimalDigits = parseInt(localStorage.getItem("moneyDecimalDigits") || "0", 10);
  const found = MONEY_FORMATS.find(f => f.key === formatKey) || MONEY_FORMATS[0];
  return {
    ...found,
    decimalDigits,
  };
}

export function setMoneyFormatSettings({ formatKey, decimalDigits }) {
  localStorage.setItem("moneyFormat", formatKey);
  localStorage.setItem("moneyDecimalDigits", decimalDigits);
}
