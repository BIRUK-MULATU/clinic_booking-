import { useEffect, useState } from "react";
import { api, REGISTRATION_MESSAGES } from "../api";
import { formatMoney } from "../utils";

const EMPTY = { name: "", dateOfBirth: "", phone: "", username: "", password: "" };

/**
 * Hospital-expansion: the clinic creates every patient login itself -
 * there is no self sign-up. Reception fills in the details here and
 * hands the patient their username and password.
 */
export default function PatientsPage() {
  const [patients, setPatients] = useState(null);
  const [form, setForm] = useState(EMPTY);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [submitting, setSubmitting] = useState(false);

  function load() {
    api
      .patients()
      .then(({ ok, data }) => ok && setPatients(data))
      .catch(() => setError("Could not reach the server."));
  }

  useEffect(load, []);

  function set(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setSubmitting(true);
    setError("");
    setNotice("");
    try {
      const { ok, data } = await api.createPatient(form);
      if (ok && data?.created) {
        setNotice(`Account created for ${data.patient.name}. Username: ${data.patient.username}`);
        setForm(EMPTY);
        load();
        return;
      }
      if (ok && data && data.created === false) {
        setError(REGISTRATION_MESSAGES[data.reason] || "The account could not be created.");
        return;
      }
      setError("The account could not be created.");
    } catch (err) {
      setError(err.message || "Could not reach the server.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="page" id="patients-page">
      <div className="page-header">
        <h1 id="page-title">Patients</h1>
        <p>Create a login for a patient and give them the username and password.</p>
      </div>

      {error && <p className="alert alert-error" id="patient-error">{error}</p>}
      {notice && <p className="alert alert-success" id="patient-notice">{notice}</p>}

      <div className="card">
        <h3 style={{ marginTop: 0 }}>New patient account</h3>
        <form id="new-patient-form" onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="patient-name">Full name</label>
            <input id="patient-name" value={form.name} onChange={(e) => set("name", e.target.value)} required />
          </div>
          <div style={{ display: "flex", gap: 12, flexWrap: "wrap" }}>
            <div className="field" style={{ flex: 1, minWidth: 160 }}>
              <label htmlFor="patient-dob">Date of birth</label>
              <input
                id="patient-dob"
                type="date"
                value={form.dateOfBirth}
                onChange={(e) => set("dateOfBirth", e.target.value)}
                required
              />
            </div>
            <div className="field" style={{ flex: 1, minWidth: 160 }}>
              <label htmlFor="patient-phone">Phone</label>
              <input id="patient-phone" value={form.phone} onChange={(e) => set("phone", e.target.value)} />
            </div>
          </div>
          <div style={{ display: "flex", gap: 12, flexWrap: "wrap" }}>
            <div className="field" style={{ flex: 1, minWidth: 160 }}>
              <label htmlFor="patient-username">Username</label>
              <input
                id="patient-username"
                value={form.username}
                onChange={(e) => set("username", e.target.value)}
                required
              />
            </div>
            <div className="field" style={{ flex: 1, minWidth: 160 }}>
              <label htmlFor="patient-password">Password (min 4 characters)</label>
              <input
                id="patient-password"
                value={form.password}
                onChange={(e) => set("password", e.target.value)}
                required
              />
            </div>
          </div>
          <button
            className="btn btn-primary btn-sm"
            id="create-patient-submit"
            type="submit"
            disabled={submitting}
            style={{ width: "auto" }}
          >
            Create account
          </button>
        </form>
      </div>

      <div className="page-header" style={{ marginTop: 32 }}>
        <h2 id="patient-list-title">Registered patients</h2>
      </div>

      {patients?.length === 0 && (
        <div className="card empty-state" id="no-patients-message">
          <div className="icon">🧑‍⚕️</div>
          No patient accounts yet.
        </div>
      )}

      <div style={{ display: "flex", flexDirection: "column", gap: 12 }} id="patients-table">
        {patients?.map((patient) => (
          <div className="card appointment-card" id={`patient-row-${patient.id}`} key={patient.id}>
            <div className="appointment-info">
              <span style={{ fontWeight: 700 }}>{patient.name}</span>
              <span style={{ color: "var(--text-muted)", fontSize: "0.85rem" }}>
                @{patient.username} · born {patient.dateOfBirth} · {patient.phone || "no phone"}
              </span>
              {patient.outstandingBalance > 0 && (
                <span className="status-pill status-REQUESTED" id={`patient-balance-${patient.id}`}>
                  Owes {formatMoney(patient.outstandingBalance)}
                </span>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
