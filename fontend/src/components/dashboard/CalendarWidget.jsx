import React, { useState } from "react";
import "../../styles/components/dashboard/CalendarWidget.css";

export default function CalendarWidget() {
  const [currentDate, setCurrentDate] = useState(new Date());

  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();

  // Lấy ngày đầu tiên của tháng và số ngày trong tháng
  const firstDay = new Date(year, month, 1);
  const lastDay = new Date(year, month + 1, 0);
  const daysInMonth = lastDay.getDate();
  const startingDayOfWeek = firstDay.getDay(); // 0 = Chủ nhật, 1 = Thứ 2, ...

  // Tên tháng và năm
  const monthNames = [
    "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
    "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"
  ];

  const dayNames = ["CN", "T2", "T3", "T4", "T5", "T6", "T7"];

  const today = new Date();
  const isToday = (day) => {
    return (
      day === today.getDate() &&
      month === today.getMonth() &&
      year === today.getFullYear()
    );
  };

  const goToPreviousMonth = () => {
    setCurrentDate(new Date(year, month - 1, 1));
  };

  const goToNextMonth = () => {
    setCurrentDate(new Date(year, month + 1, 1));
  };

  const goToToday = () => {
    setCurrentDate(new Date());
  };

  // Tạo mảng các ngày trong tháng
  const days = [];
  // Thêm các ô trống cho ngày đầu tháng
  for (let i = 0; i < startingDayOfWeek; i++) {
    days.push(null);
  }
  // Thêm các ngày trong tháng
  for (let day = 1; day <= daysInMonth; day++) {
    days.push(day);
  }

  return (
    <div className="calendar-widget">
      <div className="calendar-widget__header">
        <button
          className="calendar-widget__nav-btn"
          onClick={goToPreviousMonth}
          title="Tháng trước"
        >
          <i className="bi bi-chevron-left" />
        </button>
        <div className="calendar-widget__month-year">
          <h4>{monthNames[month]}</h4>
          <span>{year}</span>
        </div>
        <button
          className="calendar-widget__nav-btn"
          onClick={goToNextMonth}
          title="Tháng sau"
        >
          <i className="bi bi-chevron-right" />
        </button>
      </div>

      <div className="calendar-widget__body">
        <div className="calendar-widget__weekdays">
          {dayNames.map((day) => (
            <div key={day} className="calendar-widget__weekday">
              {day}
            </div>
          ))}
        </div>

        <div className="calendar-widget__days">
          {days.map((day, index) => (
            <div
              key={index}
              className={`calendar-widget__day ${
                day === null ? "calendar-widget__day--empty" : ""
              } ${isToday(day) ? "calendar-widget__day--today" : ""}`}
            >
              {day}
            </div>
          ))}
        </div>
      </div>

      <div className="calendar-widget__footer">
        <button
          className="calendar-widget__today-btn"
          onClick={goToToday}
        >
          <i className="bi bi-calendar-check me-1" />
          Hôm nay
        </button>
      </div>
    </div>
  );
}

