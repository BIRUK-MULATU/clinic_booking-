import { useCallback, useEffect, useState } from "react";
import { api, REMINDER_SKIP_MESSAGES } from "../api";
import { formatSlot } from "../utils";

/**
 * Hospital-expansion Rule F: reception's "reminders due" board. Lists every
 * CONFIRMED appointment whose slot is within the next 24 hours and lets the
 * admin text the 24-hour reminder - one row at a time, or the whole batch at
 * once. A row already reminded shows when it went out instead of a button.
 * The same ReminderPolicy that drives the automatic sweep decides here too,
 * so a "send" that is too early or already done comes back with the reason.
 */
export default function RemindersPage() {
  const [rows, setRows] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busyId, setBusyId] = useState(null);

  const load = useCallback(() => {
    api
      .remindersDue()
      .then(({ ok, data }) => ok && setRows(data))
      .catch(() => setError("Could not reach the server."));
  }, []);

  useEffect(load, [load]);

  async function sendOne(id) {
    setBusyId(id);
    setError("");
    setNotice("");
    try {
      const { ok, data } = await api.sendReminder(id);
      if (!ok) {
        setError("That reminder could not be sent.");
      } else if (data.sent) {
        setNotice("Reminder sent.");
      } else {
        setError(REMINDER_SKIP_MESSAGES[data.reason] || "Nothing was sent.");
      }
      load();
    } catch {
      setError("That reminder could not be sent.");
    } finally {
      setBusyId(null);
    }
  }

  async function sendAll() {
    setBusyId("all");
    setError("");
    setNotice("");
    try {
      const { ok, data } = await api.sendAllReminders();
      if (ok) {
        setNotice(
          data.sent === 0
            ? "Nothing was due — no reminders sent."
            : `${data.sent} reminder${data.sent === 1 ? "" : "s"} sent.`,
        );
      } else {
        setError("The reminder sweep could not be run.");
      }
      load();
    } catch {
      setError("The reminder sweep could not be run.");
    } finally {
      setBusyId(null);
    }
  }

  const pending = rows?.filter((r) => !r.reminderSentAt).length ?? 0;

  return (
    <div className="page" id="reminders-page">
      <div className="page-header">
        <h1 id="page-title">Reminders due</h1>
        <p>
          Confirmed appointments in the next 24 hours. Text each patient their reminder, or send the
          whole batch at once. Reminders also go out automatically every 15 minutes.
        </p>
      </div>

      {error && <p className="alert alert-error" id="reminders-error">{error}</p>}
      {notice && <p className="alert alert-success" id="reminders-notice">{notice}</p>}

      <button
        className="btn btn-primary"
        id="send-all-reminders-button"
        disabled={busyId === "all" || pending === 0}
        onClick={sendAll}
        style={{ marginBottom: 16 }}
      >
        Send all due reminders{pending > 0 ? ` (${pending})` : ""}
      </button>

      {rows?.length === 0 && (
        <div className="card empty-state" id="no-reminders-message">
          <div className="icon">📭</div>
          No appointments in the next 24 hours.
        </div>
      )}

      <div style={{ display: "flex", flexDirection: "column", gap: 12 }} id="reminders-table">
        {rows?.map((row) => (
          <div className="card appointment-card" id={`reminder-row-${row.id}`} key={row.id}>
            <div className="appointment-info">
              <span style={{ fontWeight: 700 }}>{row.patientName}</span>
              <span style={{ color: "var(--text-muted)", fontSize: "0.85rem" }}>{row.phone}</span>
              <span style={{ color: "var(--text-muted)", fontSize: "0.85rem" }}>
                {formatSlot(row.slotStartTime).raw}
              </span>
            </div>
            <div className="appointment-actions">
              {row.reminderSentAt ? (
                <span className="status-pill status-CONFIRMED" id={`reminder-sent-${row.id}`}>
                  Sent {formatSlot(row.reminderSentAt).timeLabel}
                </span>
              ) : (
                <button
                  className="btn btn-secondary btn-sm"
                  id={`send-reminder-${row.id}`}
                  disabled={busyId === row.id}
                  onClick={() => sendOne(row.id)}
                >
                  Send reminder
                </button>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
