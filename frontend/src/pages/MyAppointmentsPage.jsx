import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, REJECTION_MESSAGES } from "../api";
import { formatMoney, formatSlot } from "../utils";

export default function MyAppointmentsPage() {
  const [appointments, setAppointments] = useState(null);
  const [slots, setSlots] = useState([]);
  const [moveTo, setMoveTo] = useState({}); // appointmentId -> selected new slotId
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busyId, setBusyId] = useState(null);

  function load() {
    api
      .appointments()
      .then(({ ok, data }) => (ok ? setAppointments(data) : setError("Could not load appointments.")))
      .catch(() => setError("Could not reach the server."));
    api
      .slots()
      .then(({ ok, data }) => ok && setSlots(data))
      .catch(() => {});
  }

  useEffect(load, []);

  async function reschedule(id) {
    const slotId = moveTo[id];
    if (!slotId) return;
    setBusyId(id);
    setError("");
    setNotice("");
    try {
      const { ok, data } = await api.reschedule(id, Number(slotId));
      if (!ok) {
        setError("That appointment could not be moved.");
      } else if (data.approved) {
        setNotice("Appointment moved.");
        setMoveTo((m) => ({ ...m, [id]: "" }));
      } else {
        setError(REJECTION_MESSAGES[data.reason] || "That slot could not be used.");
      }
      load();
    } catch {
      setError("That appointment could not be moved.");
    } finally {
      setBusyId(null);
    }
  }

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
        <p>Reception confirms requested appointments; check in once yours is confirmed, or cancel one you no longer need.</p>
      </div>

      {error && <p className="alert alert-error">{error}</p>}
      {notice && <p className="alert alert-success">{notice}</p>}

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
          const canCheckIn = appt.status === "CONFIRMED";
          const canCancel =
            appt.status === "REQUESTED" || appt.status === "CONFIRMED" || appt.status === "WAITLISTED";
          const canReschedule = appt.status === "REQUESTED" || appt.status === "CONFIRMED";
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
                  {appt.netPayable != null && appt.netPayable !== appt.feeAmount &&
                    ` · You pay: ${formatMoney(appt.netPayable)}`}
                  {appt.cancellationFee != null && ` · Cancellation fee: ${formatMoney(appt.cancellationFee)}`}
                </span>
                {canReschedule && slots.length > 0 && (
                  <span style={{ display: "flex", gap: 6, marginTop: 4 }}>
                    <select
                      id={`reschedule-slot-${appt.id}`}
                      value={moveTo[appt.id] || ""}
                      onChange={(e) => setMoveTo((m) => ({ ...m, [appt.id]: e.target.value }))}
                    >
                      <option value="">Move to…</option>
                      {slots.map((s) => (
                        <option key={s.id} value={s.id}>
                          {formatSlot(s.startTime).raw}
                        </option>
                      ))}
                    </select>
                    <button
                      className="btn btn-secondary btn-sm"
                      id={`reschedule-button-${appt.id}`}
                      disabled={busyId === appt.id || !moveTo[appt.id]}
                      onClick={() => reschedule(appt.id)}
                    >
                      Move
                    </button>
                  </span>
                )}
              </div>
              <div className="appointment-actions">
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
