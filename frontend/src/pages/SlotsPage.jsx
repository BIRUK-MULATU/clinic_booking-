import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api, DOCTOR_LOAD } from "../api";
import { useAuth } from "../context/AuthContext";
import { formatSlot } from "../utils";
import { defaultDoctorAvatar } from "../doctorAvatars";

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
        <div style={{ display: "flex", flexDirection: "column", gap: 12 }} id="slots-table">
          {slots.map((slot) => {
            const { dateLabel, timeLabel } = formatSlot(slot.startTime);
            return (
              <div className="card appointment-card" id={`slot-row-${slot.id}`} key={slot.id}>
                <div className="appointment-info" style={{ flexDirection: "row", alignItems: "center", gap: 14 }}>
                  {slot.doctor ? (
                    <img
                      className="doctor-photo"
                      src={slot.doctor.photo || defaultDoctorAvatar(slot.doctor.id)}
                      alt={slot.doctor.name}
                    />
                  ) : (
                    <span className="doctor-photo doctor-photo--placeholder">🗓️</span>
                  )}
                  <span style={{ display: "flex", flexDirection: "column", gap: 4 }} id={`slot-time-${slot.id}`}>
                    <span style={{ fontWeight: 700 }}>
                      {dateLabel} · {timeLabel}
                    </span>
                    <span
                      style={{ color: "var(--text-muted)", fontSize: "0.85rem" }}
                      id={`slot-doctor-${slot.id}`}
                    >
                      {slot.doctor
                        ? `${slot.doctor.name} · ${slot.doctor.specialty}`
                        : "No doctor assigned"}
                    </span>
                    {slot.doctorDayStatus && slot.doctorDayStatus !== "AVAILABLE" && (
                      <span
                        className={`status-pill status-${DOCTOR_LOAD[slot.doctorDayStatus].pill}`}
                        id={`slot-doctor-load-${slot.id}`}
                        style={{ alignSelf: "flex-start" }}
                      >
                        {slot.doctorDayStatus === "FULL"
                          ? "Doctor fully booked this day"
                          : "Doctor nearly full this day"}
                      </span>
                    )}
                  </span>
                </div>
                <div className="appointment-actions">
                  <button
                    className="btn btn-secondary btn-sm"
                    id={`book-link-${slot.id}`}
                    onClick={() => navigate(`/book/${slot.id}`)}
                  >
                    Book
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
