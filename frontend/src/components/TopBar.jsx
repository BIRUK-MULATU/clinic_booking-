import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function TopBar() {
  const { patient, logout } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate("/login");
  }

  const initial = patient?.name?.trim()?.[0]?.toUpperCase() || "?";

  return (
    <header className="topbar">
      <div className="brand">
        <span className="brand-mark">+</span>
        Clinic Booking
      </div>

      {patient && (
        <nav className="nav-links">
          <NavLink to="/slots" className={({ isActive }) => (isActive ? "active" : "")}>
            Book a slot
          </NavLink>
          <NavLink to="/my-appointments" className={({ isActive }) => (isActive ? "active" : "")}>
            My appointments
          </NavLink>
          <NavLink to="/doctors" className={({ isActive }) => (isActive ? "active" : "")}>
            Doctors
          </NavLink>
          <NavLink to="/queue" className={({ isActive }) => (isActive ? "active" : "")}>
            Queue
          </NavLink>
          <span className="user-chip">
            <span className="avatar">{initial}</span>
            {patient.name}
          </span>
          <button className="logout-btn" onClick={handleLogout} id="logout-button">
            Log out
          </button>
        </nav>
      )}
    </header>
  );
}
