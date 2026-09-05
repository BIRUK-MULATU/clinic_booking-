import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api";

export default function DoctorsPage() {
  const [doctors, setDoctors] = useState(null);
  const [departments, setDepartments] = useState(null);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const [departmentName, setDepartmentName] = useState("");

  const [doctorName, setDoctorName] = useState("");
  const [specialty, setSpecialty] = useState("");
  const [departmentId, setDepartmentId] = useState("");

  function load() {
    api
      .doctors()
      .then(({ ok, data }) => (ok ? setDoctors(data) : setError("Could not load doctors.")))
      .catch(() => setError("Could not reach the server."));
    api
      .departments()
      .then(({ ok, data }) => {
        if (!ok) return;
        setDepartments(data);
        setDepartmentId((current) => current || data[0]?.id || "");
      })
      .catch(() => setError("Could not reach the server."));
  }

  useEffect(load, []);

  async function handleAddDepartment(e) {
    e.preventDefault();
    if (!departmentName.trim()) return;
    setSubmitting(true);
    setError("");
    try {
      const { ok } = await api.addDepartment(departmentName.trim());
      if (!ok) {
        setError("Could not add the department.");
        return;
      }
      setDepartmentName("");
      load();
    } catch {
      setError("Could not reach the server.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleAddDoctor(e) {
    e.preventDefault();
    if (!doctorName.trim() || !specialty.trim() || !departmentId) return;
    setSubmitting(true);
    setError("");
    try {
      const { ok } = await api.addDoctor({
        name: doctorName.trim(),
        specialty: specialty.trim(),
        departmentId: Number(departmentId),
      });
      if (!ok) {
        setError("Could not add the doctor.");
        return;
      }
      setDoctorName("");
      setSpecialty("");
      load();
    } catch {
      setError("Could not reach the server.");
    } finally {
      setSubmitting(false);
    }
  }

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

      <div style={{ display: "flex", flexDirection: "column", gap: 12, marginBottom: 16 }} id="doctors-table">
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

      <div className="card">
        <h3 style={{ marginTop: 0 }}>Add a doctor</h3>
        <form id="add-doctor-form" onSubmit={handleAddDoctor}>
          <div style={{ display: "flex", gap: 10, flexWrap: "wrap", alignItems: "flex-end" }}>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="doctor-name">Name</label>
              <input
                id="doctor-name"
                placeholder="Dr. Ada Lovelace"
                value={doctorName}
                onChange={(e) => setDoctorName(e.target.value)}
                required
              />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="doctor-specialty">Specialty</label>
              <input
                id="doctor-specialty"
                placeholder="Dermatologist"
                value={specialty}
                onChange={(e) => setSpecialty(e.target.value)}
                required
              />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="doctor-department">Department</label>
              <select id="doctor-department" value={departmentId} onChange={(e) => setDepartmentId(e.target.value)}>
                {departments?.map((department) => (
                  <option key={department.id} value={department.id}>
                    {department.name}
                  </option>
                ))}
              </select>
            </div>
            <button
              className="btn btn-primary btn-sm"
              id="add-doctor-submit"
              type="submit"
              disabled={submitting || !departmentId}
              style={{ width: "auto" }}
            >
              Add doctor
            </button>
          </div>
          {!departments?.length && (
            <p style={{ color: "var(--text-muted)", fontSize: "0.85rem", marginTop: 8 }}>
              Add a department below before adding a doctor.
            </p>
          )}
        </form>
      </div>

      <div className="card">
        <h3 style={{ marginTop: 0 }}>Add a department</h3>
        <form id="add-department-form" onSubmit={handleAddDepartment}>
          <div style={{ display: "flex", gap: 10, alignItems: "flex-end" }}>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="department-name">Name</label>
              <input
                id="department-name"
                placeholder="Dermatology"
                value={departmentName}
                onChange={(e) => setDepartmentName(e.target.value)}
                required
              />
            </div>
            <button
              className="btn btn-secondary btn-sm"
              id="add-department-submit"
              type="submit"
              disabled={submitting}
              style={{ width: "auto" }}
            >
              Add department
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
