import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api, REJECTION_MESSAGES } from "../api";
import { formatSlot } from "../utils";

export default function BookPage() {
  const { slotId } = useParams();
  const navigate = useNavigate();
  const [slot, setSlot] = useState(null);
  const [error, setError] = useState("");
  const [rejectionReason, setRejectionReason] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    api
      .slot(slotId)
      .then(({ ok, data }) => {
        if (ok) setSlot(data);
        else setError("This slot could not be found.");
      })
      .catch(() => setError("Could not reach the server."));
  }, [slotId]);

  async function handleConfirm() {
    setSubmitting(true);
    setError("");
    setRejectionReason(null);
    try {
      const { ok, status, data } = await api.book(Number(slotId));
      if (status === 401) {
        navigate("/login");
        return;
      }
      if (ok && data.approved) {
        navigate(`/confirmation/${data.appointment.id}`);
        return;
      }
      setError(REJECTION_MESSAGES[data.reason] || "This booking could not be completed.");
      setRejectionReason(data.reason);
    } catch {
      setError("Could not reach the server.");
    } finally {
      setSubmitting(false);
    }
  }

  // Hospital-expansion Phase C: a slot that's already taken can still be joined as a
  // waitlist entry - offered only for that specific rejection reason, not e.g. an
  // outstanding balance or insufficient notice, which joining a waitlist wouldn't fix.
  async function handleJoinWaitlist() {
    setSubmitting(true);
    setError("");
    try {
      const { ok, status, data } = await api.joinWaitlist(Number(slotId));
      if (status === 401) {
        navigate("/login");
        return;
      }
      if (ok) {
        navigate(`/confirmation/${data.id}`);
        return;
      }
      setError("Could not join the waitlist.");
    } catch {
      setError("Could not reach the server.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="page" id="book-page">
      <div className="page-header">
        <h1 id="page-title">Confirm Booking</h1>
        <p>Review the slot below, then confirm to request the appointment.</p>
      </div>

      {error && (
        <p className="alert alert-error" id="booking-error">
          {error}
        </p>
      )}

      <div className="card">
        {!slot && !error && <p>Loading slot…</p>}
        {slot && (
          <>
            <div className="summary-row">
              <span className="label">Slot time</span>
              <span className="value" id="slot-time">
                {formatSlot(slot.startTime).raw}
              </span>
            </div>
            <div style={{ marginTop: 20, display: "flex", gap: 10 }}>
              <button
                className="btn btn-primary"
                id="book-submit"
                onClick={handleConfirm}
                disabled={submitting}
                style={{ width: "auto" }}
              >
                {submitting ? "Booking…" : "Confirm Booking"}
              </button>
              {rejectionReason === "SLOT_UNAVAILABLE" && (
                <button
                  className="btn btn-secondary"
                  id="join-waitlist-submit"
                  onClick={handleJoinWaitlist}
                  disabled={submitting}
                  style={{ width: "auto" }}
                >
                  Join Waitlist Instead
                </button>
              )}
            </div>
          </>
        )}
      </div>

      <Link className="back-link" id="back-to-slots-link" to="/slots">
        ← Back to slots
      </Link>
    </div>
  );
}
