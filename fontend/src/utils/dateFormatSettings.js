// src/utils/dateFormatSettings.js

export const DATE_FORMATS = [
  {
    key: "dd/MM/yyyy",
    label: "dd/MM/yyyy (31/12/2025)",
    example: "31/12/2025"
  },
  {
    key: "MM/dd/yyyy",
    label: "MM/dd/yyyy (12/31/2025)",
    example: "12/31/2025"
  },
  {
    key: "yyyy-MM-dd",
    label: "yyyy-MM-dd (2025-12-31)",
    example: "2025-12-31"
  }
];

export function getDateFormatSetting() {
  return localStorage.getItem("dateFormat") || "dd/MM/yyyy";
}

export function setDateFormatSetting(formatKey) {
  localStorage.setItem("dateFormat", formatKey);
}

export function formatDate(date, formatKey) {
  if (!date) return "";
  const d = typeof date === "string" ? new Date(date) : date;
  if (isNaN(d.getTime())) return "";
  const day = String(d.getDate()).padStart(2, "0");
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const year = d.getFullYear();
  switch (formatKey) {
    case "MM/dd/yyyy":
      return `${month}/${day}/${year}`;
    case "yyyy-MM-dd":
      return `${year}-${month}-${day}`;
    case "dd/MM/yyyy":
    default:
      return `${day}/${month}/${year}`;
  }
}
