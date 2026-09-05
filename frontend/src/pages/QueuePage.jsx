import { useEffect, useState } from "react";
import { api } from "../api";
import { formatSlot } from "../utils";

const NEXT_ACTION = {
  WAITING: { label: "Call", fn: api.callNext },
  CALLED: { label: "Start Consultation", fn: api.startConsultation },
  IN_CONSULTATION: { label: "Complete", fn: api.completeConsultation },
};

export default function QueuePage() {
  const [entries, setEntries] = useState(null);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState(null);

  function load() {
    api
      .queue()
      .then(({ ok, data }) => (ok ? setEntries(data) : setError("Could not load the queue.")))
      .catch(() => setError("Could not reach the server."));
  }

  useEffect(load, []);

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
    </div>
  );
}
