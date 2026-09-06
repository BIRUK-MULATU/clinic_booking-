import { useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

const DEMO_ACCOUNTS = [
  { username: "abebe", password: "secret", label: "Abebe — adult, no balance" },
  { username: "selam", password: "secret", label: "Selam — child" },
  { username: "almaz", password: "secret", label: "Almaz — senior, outstanding balance" },
  { username: "reception", password: "secret", label: "Reception — admin" },
];

export default function LoginPage() {
  const { patient, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  if (patient) {
    return <Navigate to={location.state?.from || "/"} replace />;
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setSubmitting(true);
    const result = await login(username, password);
    setSubmitting(false);
    if (result.ok) {
      navigate("/");
    } else {
      setError(result.message);
    }
  }

  function fillDemo(account) {
    setUsername(account.username);
    setPassword(account.password);
    setError("");
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="brand-mark">+</div>
        <h1 id="page-title">Clinic Booking</h1>
        <p className="subtitle">Sign in to view slots and manage your appointments</p>

        {error && (
          <p className="alert alert-error" id="login-error">
            {error}
          </p>
        )}

        <form id="login-form" onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="username">Username</label>
            <input
              id="username"
              name="username"
              autoComplete="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
          </div>
          <div className="field">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              name="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
          <button className="btn btn-primary" type="submit" id="login-submit" disabled={submitting}>
            {submitting ? "Signing in…" : "Log in"}
          </button>
        </form>

        <div className="demo-accounts">
          Demo accounts (password <code>secret</code> for all):
          <ul>
            {DEMO_ACCOUNTS.map((account) => (
              <li key={account.username}>
                <button type="button" onClick={() => fillDemo(account)}>
                  {account.label}
                </button>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  );
}
