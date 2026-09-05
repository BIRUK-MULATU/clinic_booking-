import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../api";
import { formatMoney, formatSlot } from "../utils";

export default function ConfirmationPage() {
  const { appointmentId } = useParams();
  const [appointment, setAppointment] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api
      .appointment(appointmentId)
      .then(({ ok, data }) => (ok ? setAppointment(data) : setError("Appointment not found.")))
      .catch(() => setError("Could not reach the server."));
  }, [appointmentId]);

  return (
    <div className="page" id="confirmation-page">
      {error && (
        <p className="alert alert-error" id="confirmation-error">
          {error}
        </p>
      )}

      {appointment && (
        <div className="card">
          <div className="confirmation-hero">
            <div className="icon">✓</div>
            <h1 id="page-title" style={{ margin: 0 }}>
              Booking Requested
            </h1>
            <p style={{ color: "var(--text-muted)", margin: "6px 0 0" }}>
              We've held this slot for you pending confirmation by the clinic.
            </p>
          </div>

          <div className="summary-row">
            <span className="label">Status</span>
            <span className={`status-pill status-${appointment.status}`} id="confirmation-status">
              {appointment.status}
            </span>
          </div>
          <div className="summary-row">
            <span className="label">Slot</span>
            <span className="value" id="confirmation-slot-time">
              {formatSlot(appointment.slotStartTime).raw}
            </span>
          </div>
          <div className="summary-row">
            <span className="label">Category</span>
            <span className="value" id="confirmation-fee-category">
              {appointment.feeCategory}
            </span>
          </div>
          <div className="summary-row">
            <span className="label">Fee</span>
            <span className="fee-amount" id="confirmation-fee-amount">
              {formatMoney(appointment.feeAmount)}
            </span>
          </div>
        </div>
      )}

      <Link className="back-link" id="my-appointments-link" to="/my-appointments">
        Go to my appointments →
      </Link>
    </div>
  );
}
