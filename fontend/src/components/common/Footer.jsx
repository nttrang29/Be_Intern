// src/components/common/Footer.jsx
import "../../styles/Footer.css";

export default function Footer() {
  return (
    <footer className="app-footer">
      <div className="container text-center py-3">
        <small>&copy; {new Date().getFullYear()} Attendance System. All rights reserved.</small>
      </div>
    </footer>
  );
}
