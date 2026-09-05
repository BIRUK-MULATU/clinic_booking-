import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api";
import { formatMoney, formatSlot } from "../utils";

export default function MyAppointmentsPage() {
  const [appointments, setAppointments] = useState(null);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState(null);

  function load() {
    api
      .appointments()
      .then(({ ok, data }) => (ok ? setAppointments(data) : setError("Could not load appointments.")))
      .catch(() => setError("Could not reach the server."));
  }

  useEffect(load, []);

  async function act(action, id) {
    setBusyId(id);
    setError("");
    try {
      await action(id);
      load();
    } catch {
      setError("That action could not be completed.");
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="page" id="my-appointments-page">
      <div className="page-header">
        <h1 id="page-title">My Appointments</h1>
        <p>Confirm a requested appointment, or cancel one you no longer need.</p>
      </div>

      {error && <p className="alert alert-error">{error}</p>}

      <Link className="back-link" id="slots-link" to="/slots" style={{ marginTop: 0, marginBottom: 18 }}>
        + Book another slot
      </Link>

      {appointments?.length === 0 && (
        <div className="card empty-state" id="no-appointments-message">
          <div className="icon">📋</div>
          You have no appointments yet.
        </div>
      )}

      <div style={{ display: "flex", flexDirection: "column", gap: 12 }} id="appointments-table">
        {appointments?.map((appt) => {
          const { raw } = formatSlot(appt.slotStartTime);
          const canConfirm = appt.status === "REQUESTED";
          const canCheckIn = appt.status === "CONFIRMED";
          const canCancel =
            appt.status === "REQUESTED" || appt.status === "CONFIRMED" || appt.status === "WAITLISTED";
          const cancelLabel = appt.status === "WAITLISTED" ? "Leave Waitlist" : "Cancel";
          return (
            <div className="card appointment-card" id={`appointment-row-${appt.id}`} key={appt.id}>
              <div className="appointment-info">
                <span style={{ fontWeight: 700 }}>{raw}</span>
                <span className={`status-pill status-${appt.status}`} id={`appointment-status-${appt.id}`}>
                  {appt.status}
                </span>
                <span id={`appointment-fee-${appt.id}`} style={{ color: "var(--text-muted)", fontSize: "0.85rem" }}>
                  Fee: {formatMoney(appt.feeAmount)}
                  {appt.cancellationFee != null && ` · Cancellation fee: ${formatMoney(appt.cancellationFee)}`}
                </span>
              </div>
              <div className="appointment-actions">
                {canConfirm && (
                  <button
                    className="btn btn-secondary btn-sm"
                    id={`confirm-button-${appt.id}`}
                    disabled={busyId === appt.id}
                    onClick={() => act(api.confirm, appt.id)}
                  >
                    Confirm
                  </button>
                )}
                {canCheckIn && (
                  <button
                    className="btn btn-secondary btn-sm"
                    id={`check-in-button-${appt.id}`}
                    disabled={busyId === appt.id}
                    onClick={() => act(api.checkIn, appt.id)}
                  >
                    Check In
                  </button>
                )}
                {canCancel && (
                  <button
                    className="btn btn-danger btn-sm"
                    id={`cancel-button-${appt.id}`}
                    disabled={busyId === appt.id}
                    onClick={() => act(api.cancel, appt.id)}
                  >
                    {cancelLabel}
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
