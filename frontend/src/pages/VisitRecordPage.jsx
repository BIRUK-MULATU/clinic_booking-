import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api, VISIT_REJECTION_MESSAGES } from "../api";
import { formatSlot } from "../utils";

/**
 * Hospital-expansion Phase E: reception records what happened at a
 * visit - diagnosis (required), notes and prescription (optional) -
 * against an ATTENDED appointment. Once saved it is read-only; the
 * backend @OneToOne allows only one record per appointment.
 */
export default function VisitRecordPage() {
  const { appointmentId } = useParams();
  const [appointment, setAppointment] = useState(null);
  const [record, setRecord] = useState(null);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const [diagnosis, setDiagnosis] = useState("");
  const [notes, setNotes] = useState("");
  const [prescription, setPrescription] = useState("");

  function load() {
    api
      .appointment(appointmentId)
      .then(({ ok, data }) => ok && setAppointment(data))
      .catch(() => setError("Could not reach the server."));
    api
      .visitRecord(appointmentId)
      .then(({ ok, data }) => setRecord(ok ? data : null))
      .catch(() => setError("Could not reach the server."));
  }

  useEffect(load, [appointmentId]);

  async function handleSubmit(e) {
    e.preventDefault();
    if (!diagnosis.trim()) return;
    setSubmitting(true);
    setError("");
    try {
      const { ok, data } = await api.addVisitRecord(appointmentId, {
        diagnosis: diagnosis.trim(),
        notes: notes.trim(),
        prescription: prescription.trim(),
      });
      if (ok && data?.recorded) {
        setRecord(data.record);
        return;
      }
      if (ok && data && data.recorded === false) {
        setError(VISIT_REJECTION_MESSAGES[data.reason] || "The visit could not be recorded.");
        return;
      }
      setError("The visit could not be recorded.");
    } catch {
      setError("Could not reach the server.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="page" id="visit-record-page">
      <div className="page-header">
        <h1 id="page-title">Visit Record</h1>
        <p>
          Hospital-expansion Phase E — the clinical record for one attended appointment.
          {appointment && (
            <>
              {" "}
              Appointment {formatSlot(appointment.slotStartTime).raw} · {appointment.status}.
            </>
          )}
        </p>
      </div>

      {error && (
        <p className="alert alert-error" id="visit-record-error">
          {error}
        </p>
      )}

      {record ? (
        <div className="card" id="visit-record-view">
          <div className="summary-row">
            <span className="label">Diagnosis</span>
            <span className="value" id="visit-record-diagnosis">
              {record.diagnosis}
            </span>
          </div>
          <div className="summary-row">
            <span className="label">Notes</span>
            <span className="value" id="visit-record-notes">
              {record.notes || "—"}
            </span>
          </div>
          <div className="summary-row">
            <span className="label">Prescription</span>
            <span className="value" id="visit-record-prescription">
              {record.prescription || "—"}
            </span>
          </div>
          <div className="summary-row">
            <span className="label">Recorded</span>
            <span className="value">{formatSlot(record.recordedAt).raw}</span>
          </div>
        </div>
      ) : (
        <div className="card">
          <form id="visit-record-form" onSubmit={handleSubmit}>
            <div className="field">
              <label htmlFor="visit-diagnosis">Diagnosis (required, up to 500 characters)</label>
              <textarea
                id="visit-diagnosis"
                rows={2}
                maxLength={500}
                value={diagnosis}
                onChange={(e) => setDiagnosis(e.target.value)}
                required
              />
            </div>
            <div className="field">
              <label htmlFor="visit-notes">Notes (optional)</label>
              <textarea
                id="visit-notes"
                rows={3}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
              />
            </div>
            <div className="field">
              <label htmlFor="visit-prescription">Prescription (optional)</label>
              <textarea
                id="visit-prescription"
                rows={2}
                value={prescription}
                onChange={(e) => setPrescription(e.target.value)}
              />
            </div>
            <button
              className="btn btn-primary btn-sm"
              id="visit-record-submit"
              type="submit"
              disabled={submitting || !diagnosis.trim()}
              style={{ width: "auto" }}
            >
              Save visit record
            </button>
          </form>
        </div>
      )}

      <Link className="back-link" id="back-to-queue-link" to="/queue">
        ← Back to the queue
      </Link>
    </div>
  );
}
