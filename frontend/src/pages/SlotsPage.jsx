import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api";
import { useAuth } from "../context/AuthContext";
import { formatSlot } from "../utils";

export default function SlotsPage() {
  const { patient } = useAuth();
  const navigate = useNavigate();
  const [slots, setSlots] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    api
      .slots()
      .then(({ ok, data }) => {
        if (cancelled) return;
        if (ok) setSlots(data);
        else setError("Could not load slots.");
      })
      .catch(() => !cancelled && setError("Could not reach the server."));
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="page" id="slots-page">
      <div className="page-header">
        <h1 id="page-title">Available Appointment Slots</h1>
        <p>
          Welcome, <span id="patient-name">{patient?.name}</span>. Pick a slot below to request a
          booking.
        </p>
      </div>

      {error && <p className="alert alert-error">{error}</p>}

      {slots === null && !error && (
        <div className="loading-screen" style={{ minHeight: 160 }}>
          <span className="spinner" />
          Loading slots…
        </div>
      )}

      {slots?.length === 0 && (
        <div className="card empty-state" id="no-slots-message">
          <div className="icon">🗓️</div>
          No slots available right now — please check back later.
        </div>
      )}

      {slots?.length > 0 && (
        <div className="slot-list" id="slots-table">
          {slots.map((slot) => {
            const { dateLabel, timeLabel } = formatSlot(slot.startTime);
            return (
              <div className="slot-row" id={`slot-row-${slot.id}`} key={slot.id}>
                <div className="slot-time" id={`slot-time-${slot.id}`}>
                  <span className="slot-date">{dateLabel}</span>
                  <span className="slot-clock">{timeLabel}</span>
                  {slot.doctor && (
                    <span className="slot-clock" id={`slot-doctor-${slot.id}`}>
                      {slot.doctor.name} · {slot.doctor.specialty}
                    </span>
                  )}
                </div>
                <button
                  className="btn btn-secondary btn-sm"
                  id={`book-link-${slot.id}`}
                  onClick={() => navigate(`/book/${slot.id}`)}
                >
                  Book
                </button>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
