import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
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
  const [pending, setPending] = useState(null);
  const [entries, setEntries] = useState(null);
  const [roster, setRoster] = useState(null);
  const [date, setDate] = useState(today());
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState(null);

  const loadPending = useCallback(() => {
    api
      .pendingAppointments()
      .then(({ ok, data }) => ok && setPending(data))
      .catch(() => setError("Could not reach the server."));
  }, []);

  const loadQueue = useCallback(() => {
    api
      .queue()
      .then(({ ok, data }) => ok && setEntries(data))
      .catch(() => setError("Could not reach the server."));
  }, []);

  const loadRoster = useCallback(() => {
    api
      .adminAppointments(date)
      .then(({ ok, data }) => ok && setRoster(data))
      .catch(() => setError("Could not reach the server."));
  }, [date]);

  useEffect(loadPending, [loadPending]);
  useEffect(loadQueue, [loadQueue]);
  useEffect(loadRoster, [loadRoster]);

  // One helper for every button on this page: call the API, surface a failure, then
  // refresh all three lists since a single action (confirm, check in, complete) can
  // move an appointment between them.
  async function run(id, action) {
    setBusyId(id);
    setError("");
    try {
      const { ok, data } = await action(id);
      if (!ok) {
        setError(data?.message || "That action could not be completed.");
        return;
      }
      loadPending();
      loadQueue();
      loadRoster();
    } catch {
      setError("That action could not be completed.");
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="page" id="queue-page">
      <div className="page-header">
        <h1 id="page-title">Reception</h1>
        <p>Confirm requested appointments, check patients in, and run the day's queue.</p>
      </div>

      {error && <p className="alert alert-error">{error}</p>}

      <div className="page-header" style={{ marginTop: 8 }}>
        <h2 id="pending-title">Requests awaiting confirmation</h2>
        <p>Every appointment a patient has requested, any date, soonest first. Confirm to hold the slot.</p>
      </div>

      {pending?.length === 0 && (
        <div className="card empty-state" id="no-pending-message">
          <div className="icon">✅</div>
          No requests waiting.
        </div>
      )}

      <div style={{ display: "flex", flexDirection: "column", gap: 12 }} id="pending-table">
        {pending?.map((appt) => (
          <div className="card appointment-card" id={`pending-row-${appt.id}`} key={appt.id}>
            <div className="appointment-info">
              <span style={{ fontWeight: 700 }}>{appt.patientName}</span>
              <span style={{ color: "var(--text-muted)", fontSize: "0.85rem" }}>
                {formatSlot(appt.slotStartTime).raw}
              </span>
              <span className={`status-pill status-${appt.status}`} id={`pending-status-${appt.id}`}>
                {appt.status}
              </span>
              <span style={{ color: "var(--text-muted)", fontSize: "0.85rem" }}>
                Fee: {formatMoney(appt.feeAmount)}
              </span>
            </div>
            <div className="appointment-actions">
              <button
                className="btn btn-primary btn-sm"
                id={`pending-confirm-${appt.id}`}
                disabled={busyId === appt.id}
                onClick={() => run(appt.id, api.confirm)}
              >
                Confirm
              </button>
            </div>
          </div>
        ))}
      </div>

      <div className="page-header" style={{ marginTop: 36 }}>
        <h2 id="queue-title">Live queue</h2>
        <p>Every checked-in patient not yet seen, oldest check-in first.</p>
      </div>

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
                    onClick={() => run(entry.id, next.fn)}
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
        <h2 id="roster-title">Appointments by date</h2>
        <p>Every patient expected on the chosen date, in slot order — checked in or not yet.</p>
      </div>

      <div className="field" style={{ maxWidth: 220, marginBottom: 16 }}>
        <label htmlFor="roster-date">Date</label>
        <input id="roster-date" type="date" value={date} onChange={(e) => setDate(e.target.value)} />
      </div>

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
                  disabled={busyId === appt.id}
                  onClick={() => run(appt.id, api.confirm)}
                >
                  Confirm
                </button>
              )}
              {appt.status === "CONFIRMED" && (
                <button
                  className="btn btn-secondary btn-sm"
                  id={`roster-check-in-${appt.id}`}
                  disabled={busyId === appt.id}
                  onClick={() => run(appt.id, api.checkIn)}
                >
                  Check In
                </button>
              )}
              {appt.status === "ATTENDED" && (
                <Link
                  className="btn btn-secondary btn-sm"
                  id={`roster-visit-record-${appt.id}`}
                  to={`/appointments/${appt.id}/visit-record`}
                >
                  Visit Record
                </Link>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
