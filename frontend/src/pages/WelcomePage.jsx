import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

/**
 * The landing page shown at "/" right after login. Greets the signed-in
 * user by name and offers the quick actions that make sense for their
 * role. Not part of the graded Thymeleaf app - this is the React UI only.
 */

function greeting() {
  const h = new Date().getHours();
  if (h < 12) return "Good morning";
  if (h < 18) return "Good afternoon";
  return "Good evening";
}

const PATIENT_ACTIONS = [
  { to: "/slots", icon: "🗓️", title: "Book a slot", text: "See the available appointment times and request one." },
  { to: "/my-appointments", icon: "📋", title: "My appointments", text: "Confirm, check in, reschedule or cancel your bookings." },
];

const ADMIN_ACTIONS = [
  { to: "/queue", icon: "🏥", title: "Reception", text: "Confirm requests, check patients in, run the live queue." },
  { to: "/manage-slots", icon: "🕓", title: "Slots", text: "Add, edit or remove bookable appointment times." },
  { to: "/patients", icon: "🧑‍⚕️", title: "Patients", text: "Create patient logins and set insurance coverage." },
  { to: "/doctors", icon: "👩‍⚕️", title: "Doctors", text: "Doctors, departments, daily limits and availability." },
  { to: "/reminders", icon: "🔔", title: "Reminders", text: "Text patients their 24-hour appointment reminder." },
];

export default function WelcomePage() {
  const { patient } = useAuth();
  const name = patient?.name || "there";
  const isAdmin = patient?.role === "ADMIN";
  const actions = isAdmin ? ADMIN_ACTIONS : PATIENT_ACTIONS;

  return (
    <div className="page" id="welcome-page">
      <div className="card confirmation-hero" style={{ marginBottom: 20 }}>
        <div className="icon">👋</div>
        <h1 id="welcome-title" style={{ margin: 0 }}>
          {greeting()}, <span id="welcome-name">{name}</span>
        </h1>
        <p style={{ color: "var(--text-muted)", margin: "6px 0 0" }} id="welcome-subtitle">
          {isAdmin
            ? "You're signed in to the reception desk. Here's what you can do."
            : "Welcome to the clinic booking system. Here's where to start."}
        </p>
      </div>

      <div
        id="welcome-actions"
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fill, minmax(240px, 1fr))",
          gap: 14,
        }}
      >
        {actions.map((a) => (
          <Link
            key={a.to}
            to={a.to}
            id={`welcome-link-${a.to.replace(/\W/g, "")}`}
            className="card"
            style={{ textDecoration: "none", color: "inherit", display: "block" }}
          >
            <div style={{ fontSize: "1.6rem", marginBottom: 6 }}>{a.icon}</div>
            <div style={{ fontWeight: 700, marginBottom: 2 }}>{a.title}</div>
            <div style={{ color: "var(--text-muted)", fontSize: "0.88rem" }}>{a.text}</div>
          </Link>
        ))}
      </div>
    </div>
  );
}
