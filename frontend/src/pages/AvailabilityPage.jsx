import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../api";

const DAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];

export default function AvailabilityPage() {
  const { doctorId } = useParams();
  const [rules, setRules] = useState(null);
  const [exceptions, setExceptions] = useState(null);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const [dayOfWeek, setDayOfWeek] = useState("MONDAY");
  const [startTime, setStartTime] = useState("09:00");
  const [endTime, setEndTime] = useState("12:00");
  const [slotDurationMinutes, setSlotDurationMinutes] = useState(30);
  const [exceptionDate, setExceptionDate] = useState("");

  function load() {
    api
      .doctorAvailability(doctorId)
      .then(({ ok, data }) => ok && setRules(data))
      .catch(() => setError("Could not reach the server."));
    api
      .doctorExceptions(doctorId)
      .then(({ ok, data }) => ok && setExceptions(data))
      .catch(() => setError("Could not reach the server."));
  }

  useEffect(load, [doctorId]);

  async function handleAddRule(e) {
    e.preventDefault();
    setSubmitting(true);
    setError("");
    try {
      const { ok } = await api.addAvailabilityRule(doctorId, {
        dayOfWeek,
        startTime,
        endTime,
        slotDurationMinutes: Number(slotDurationMinutes),
      });
      if (!ok) {
        setError("Could not add the rule.");
        return;
      }
      load();
    } catch {
      setError("Could not reach the server.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleAddException(e) {
    e.preventDefault();
    if (!exceptionDate) return;
    setSubmitting(true);
    setError("");
    try {
      const { ok } = await api.addException(doctorId, exceptionDate);
      if (!ok) {
        setError("Could not add the exception.");
        return;
      }
      setExceptionDate("");
      load();
    } catch {
      setError("Could not reach the server.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="page" id="availability-page">
      <div className="page-header">
        <h1 id="page-title">Doctor Availability</h1>
        <p>
          Add a weekly recurring rule or a one-off exception (leave/holiday). Saving either
          regenerates this doctor's slots for the next 14 days.
        </p>
      </div>

      {error && <p className="alert alert-error">{error}</p>}

      <div className="card">
        <h3 style={{ marginTop: 0 }}>Weekly rules</h3>
        {rules?.length === 0 && <p style={{ color: "var(--text-muted)" }}>No rules yet.</p>}
        <div id="availability-rules-table" style={{ display: "flex", flexDirection: "column", gap: 8, marginBottom: 16 }}>
          {rules?.map((rule) => (
            <div className="summary-row" id={`availability-rule-${rule.id}`} key={rule.id}>
              <span className="label">{rule.dayOfWeek}</span>
              <span className="value">
                {rule.startTime}–{rule.endTime} ({rule.slotDurationMinutes} min slots)
              </span>
            </div>
          ))}
        </div>

        <form id="add-availability-form" onSubmit={handleAddRule}>
          <div style={{ display: "flex", gap: 10, flexWrap: "wrap", alignItems: "flex-end" }}>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="day-of-week">Day</label>
              <select id="day-of-week" value={dayOfWeek} onChange={(e) => setDayOfWeek(e.target.value)}>
                {DAYS.map((day) => (
                  <option key={day} value={day}>
                    {day}
                  </option>
                ))}
              </select>
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="start-time">Start</label>
              <input id="start-time" type="time" value={startTime} onChange={(e) => setStartTime(e.target.value)} />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="end-time">End</label>
              <input id="end-time" type="time" value={endTime} onChange={(e) => setEndTime(e.target.value)} />
            </div>
            <div className="field" style={{ marginBottom: 0, width: 110 }}>
              <label htmlFor="slot-duration">Minutes</label>
              <input
                id="slot-duration"
                type="number"
                min="5"
                step="5"
                value={slotDurationMinutes}
                onChange={(e) => setSlotDurationMinutes(e.target.value)}
              />
            </div>
            <button className="btn btn-primary btn-sm" id="add-availability-submit" type="submit"
                    disabled={submitting} style={{ width: "auto" }}>
              Add rule
            </button>
          </div>
        </form>
      </div>

      <div className="card">
        <h3 style={{ marginTop: 0 }}>Exceptions (leave / holidays)</h3>
        {exceptions?.length === 0 && <p style={{ color: "var(--text-muted)" }}>No exceptions yet.</p>}
        <div id="exceptions-table" style={{ display: "flex", flexDirection: "column", gap: 8, marginBottom: 16 }}>
          {exceptions?.map((exception) => (
            <div className="summary-row" id={`exception-${exception.id}`} key={exception.id}>
              <span className="value">{exception.date}</span>
            </div>
          ))}
        </div>

        <form id="add-exception-form" onSubmit={handleAddException}>
          <div style={{ display: "flex", gap: 10, alignItems: "flex-end" }}>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="exception-date">Date</label>
              <input
                id="exception-date"
                type="date"
                value={exceptionDate}
                onChange={(e) => setExceptionDate(e.target.value)}
                required
              />
            </div>
            <button className="btn btn-secondary btn-sm" id="add-exception-submit" type="submit"
                    disabled={submitting} style={{ width: "auto" }}>
              Add exception
            </button>
          </div>
        </form>
      </div>

      <Link className="back-link" id="back-to-doctors-link" to="/doctors">
        ← Back to doctors
      </Link>
    </div>
  );
}
