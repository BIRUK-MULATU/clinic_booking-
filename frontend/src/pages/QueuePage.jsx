import { useEffect, useState } from "react";
import { api } from "../api";
import { formatMoney, formatSlot } from "../utils";

const NEXT_ACTION = {
  WAITING: { label: "Call", fn: api.callNext },
  CALLED: { label: "Start Consultation", fn: api.startConsultation },
  IN_CONSULTATION: { label: "Complete", fn: api.completeConsultation },
};

function today() {
  const now = new Date();
  const offset = now.getTimezoneOffset() * 60000;
  return new Date(now - offset).toISOString().slice(0, 10);
}

export default function QueuePage() {
  const [entries, setEntries] = useState(null);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState(null);

  const [date, setDate] = useState(today());
  const [roster, setRoster] = useState(null);
  const [rosterError, setRosterError] = useState("");
  const [rosterBusyId, setRosterBusyId] = useState(null);

  function load() {
    api
      .queue()
      .then(({ ok, data }) => (ok ? setEntries(data) : setError("Could not load the queue.")))
      .catch(() => setError("Could not reach the server."));
  }

  function loadRoster() {
    api
      .adminAppointments(date)
      .then(({ ok, data }) => (ok ? setRoster(data) : setRosterError("Could not load today's appointments.")))
      .catch(() => setRosterError("Could not reach the server."));
  }

  useEffect(load, []);
  useEffect(loadRoster, [date]);

  async function advance(entryId, action) {
    setBusyId(entryId);
    setError("");
    try {
      const { ok, data } = await action(entryId);
      if (!ok) {
        setError(data?.message || "That action could not be completed.");
        return;
      }
      load();
    } catch {
      setError("That action could not be completed.");
    } finally {
      setBusyId(null);
    }
  }

  async function confirmAppointment(appointmentId) {
    setRosterBusyId(appointmentId);
    setRosterError("");
    try {
      const { ok, data } = await api.confirm(appointmentId);
      if (!ok) {
        setRosterError(data?.message || "That appointment could not be confirmed.");
        return;
      }
      loadRoster();
    } catch {
      setRosterError("Could not reach the server.");
    } finally {
      setRosterBusyId(null);
    }
  }

  return (
    <div className="page" id="queue-page">
      <div className="page-header">
        <h1 id="page-title">Queue</h1>
        <p>Hospital-expansion Phase D — every checked-in patient not yet seen, oldest first.</p>
      </div>

      {error && <p className="alert alert-error">{error}</p>}

      {entries?.length === 0 && (
        <div className="card empty-state" id="no-queue-entries-message">
          <div className="icon">🪑</div>
          Nobody is checked in right now.
        </div>
      )}

      <div style={{ display: "flex", flexDirection: "column", gap: 12 }} id="queue-table">
        {entries?.map((entry) => {
          const next = NEXT_ACTION[entry.status];
          return (
            <div className="card appointment-card" id={`queue-entry-${entry.id}`} key={entry.id}>
              <div className="appointment-info">
                <span style={{ fontWeight: 700 }}>{entry.patientName}</span>
                <span style={{ color: "var(--text-muted)", fontSize: "0.85rem" }}>
                  Appointment: {formatSlot(entry.slotStartTime).raw}
                </span>
                <span className={`status-pill status-${entry.status}`} id={`queue-status-${entry.id}`}>
                  {entry.status}
                </span>
              </div>
              <div className="appointment-actions">
                {next && (
                  <button
                    className="btn btn-secondary btn-sm"
                    id={`queue-advance-${entry.id}`}
                    disabled={busyId === entry.id}
                    onClick={() => advance(entry.id, next.fn)}
                  >
                    {next.label}
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>

      <div className="page-header" style={{ marginTop: 36 }}>
        <h2 id="roster-title">Today's Appointments</h2>
        <p>Every patient expected on the chosen date, in slot order — checked in or not yet.</p>
      </div>

      <div className="field" style={{ maxWidth: 220, marginBottom: 16 }}>
        <label htmlFor="roster-date">Date</label>
        <input id="roster-date" type="date" value={date} onChange={(e) => setDate(e.target.value)} />
      </div>

      {rosterError && <p className="alert alert-error">{rosterError}</p>}

      {roster?.length === 0 && (
        <div className="card empty-state" id="no-roster-message">
          <div className="icon">📅</div>
          No appointments on this date.
        </div>
      )}

      <div style={{ display: "flex", flexDirection: "column", gap: 12 }} id="roster-table">
        {roster?.map((appt) => (
          <div className="card appointment-card" id={`roster-row-${appt.id}`} key={appt.id}>
            <div className="appointment-info">
              <span style={{ fontWeight: 700 }}>{appt.patientName}</span>
              <span style={{ color: "var(--text-muted)", fontSize: "0.85rem" }}>
                {formatSlot(appt.slotStartTime).raw}
              </span>
              <span className={`status-pill status-${appt.status}`} id={`roster-status-${appt.id}`}>
                {appt.status}
              </span>
              <span style={{ color: "var(--text-muted)", fontSize: "0.85rem" }}>
                Fee: {formatMoney(appt.feeAmount)}
              </span>
            </div>
            <div className="appointment-actions">
              {appt.status === "REQUESTED" && (
                <button
                  className="btn btn-secondary btn-sm"
                  id={`roster-confirm-${appt.id}`}
                  disabled={rosterBusyId === appt.id}
                  onClick={() => confirmAppointment(appt.id)}
                >
                  Confirm
                </button>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
