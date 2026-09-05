import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api";

export default function DoctorsPage() {
  const [doctors, setDoctors] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api
      .doctors()
      .then(({ ok, data }) => (ok ? setDoctors(data) : setError("Could not load doctors.")))
      .catch(() => setError("Could not reach the server."));
  }, []);

  return (
    <div className="page" id="doctors-page">
      <div className="page-header">
        <h1 id="page-title">Doctors</h1>
        <p>Hospital-expansion Phase A/B — doctors, departments, and their weekly availability.</p>
      </div>

      {error && <p className="alert alert-error">{error}</p>}

      {doctors?.length === 0 && (
        <div className="card empty-state" id="no-doctors-message">
          <div className="icon">🩺</div>
          No doctors yet.
        </div>
      )}

      <div style={{ display: "flex", flexDirection: "column", gap: 12 }} id="doctors-table">
        {doctors?.map((doctor) => (
          <div className="card appointment-card" id={`doctor-row-${doctor.id}`} key={doctor.id}>
            <div className="appointment-info">
              <span style={{ fontWeight: 700 }}>{doctor.name}</span>
              <span style={{ color: "var(--text-muted)", fontSize: "0.85rem" }}>
                {doctor.specialty} · {doctor.departmentName}
              </span>
            </div>
            <div className="appointment-actions">
              <Link
                className="btn btn-secondary btn-sm"
                id={`manage-availability-link-${doctor.id}`}
                to={`/doctors/${doctor.id}/availability`}
              >
                Manage Availability
              </Link>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
