import { useCallback, useEffect, useState } from "react";
import { api, DOCTOR_LOAD, REJECTION_MESSAGES } from "../api";
import { formatSlot } from "../utils";
import ConfirmButton from "../components/ConfirmButton";

/**
 * Hospital-expansion: reception adds one-off appointment slots (on top
 * of whatever each doctor's weekly availability generates) and can book
 * a slot directly onto a patient.
 */
export default function ManageSlotsPage() {
  const [slots, setSlots] = useState(null);
  const [doctors, setDoctors] = useState([]);
  const [patients, setPatients] = useState([]);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);

  const [slotDoctorId, setSlotDoctorId] = useState("");
  const [slotStart, setSlotStart] = useState("");

  const [bookPatientId, setBookPatientId] = useState("");
  const [bookSlotId, setBookSlotId] = useState("");

  const [editing, setEditing] = useState(null); // { id, doctorId, startTime }

  const load = useCallback(() => {
    api.adminSlots().then(({ ok, data }) => ok && setSlots(data)).catch(() => setError("Could not reach the server."));
    api.doctors().then(({ ok, data }) => ok && setDoctors(data)).catch(() => {});
    api.patients().then(({ ok, data }) => ok && setPatients(data)).catch(() => {});
  }, []);

  useEffect(load, [load]);

  const isTaken = (slot) => slot.booked;

  async function handleAddSlot(e) {
    e.preventDefault();
    if (!slotStart) return;
    setBusy(true);
    setError("");
    setNotice("");
    try {
      const { ok, data } = await api.addSlot({
        doctorId: slotDoctorId ? Number(slotDoctorId) : null,
        startTime: slotStart,
      });
      if (!ok) {
        setError(data?.message || "The slot could not be added.");
        return;
      }
      if (data?.warning) {
        setError(`Slot added — but ${data.warning}`);
      } else {
        setNotice("Slot added.");
      }
      setSlotStart("");
      load();
    } catch (err) {
      setError(err.message || "Could not reach the server.");
    } finally {
      setBusy(false);
    }
  }

  function startEdit(slot) {
    setError("");
    setNotice("");
    // datetime-local wants "yyyy-MM-ddTHH:mm"
    setEditing({ id: slot.id, doctorId: slot.doctor?.id ? String(slot.doctor.id) : "", startTime: slot.startTime.slice(0, 16) });
  }

  async function saveEdit() {
    setError("");
    try {
      const { ok, data } = await api.updateSlot(editing.id, {
        doctorId: editing.doctorId ? Number(editing.doctorId) : null,
        startTime: editing.startTime,
      });
      if (!ok) {
        setError(data?.message || "Could not update the slot.");
        return;
      }
      setEditing(null);
      setNotice("Slot updated.");
      load();
    } catch (err) {
      setError(err.message || "Could not reach the server.");
    }
  }

  async function removeSlot(id) {
    setError("");
    try {
      const { ok, data } = await api.deleteSlot(id);
      if (!ok) {
        setError(data?.message || "Could not delete the slot.");
        return;
      }
      setNotice("Slot deleted.");
      load();
    } catch (err) {
      setError(err.message || "Could not reach the server.");
    }
  }

  async function handleBook(e) {
    e.preventDefault();
    if (!bookPatientId || !bookSlotId) return;
    setBusy(true);
    setError("");
    setNotice("");
    try {
      const { ok, data } = await api.bookForPatient(Number(bookPatientId), Number(bookSlotId));
      if (ok && data?.approved) {
        setNotice("Appointment booked and confirmed.");
        setBookSlotId("");
        load();
        return;
      }
      if (ok && data && data.approved === false) {
        setError(REJECTION_MESSAGES[data.reason] || "That slot could not be booked.");
        return;
      }
      setError("That slot could not be booked.");
    } catch (err) {
      setError(err.message || "Could not reach the server.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="page" id="manage-slots-page">
      <div className="page-header">
        <h1 id="page-title">Slots</h1>
        <p>Add appointment slots and book them onto patients. Doctors also generate slots from their weekly schedule.</p>
      </div>

      {error && <p className="alert alert-error" id="slots-error">{error}</p>}
      {notice && <p className="alert alert-success" id="slots-notice">{notice}</p>}

      <div className="card">
        <h3 style={{ marginTop: 0 }}>Add a slot</h3>
        <form id="add-slot-form" onSubmit={handleAddSlot}>
          <div style={{ display: "flex", gap: 12, flexWrap: "wrap", alignItems: "flex-end" }}>
            <div className="field" style={{ marginBottom: 0, minWidth: 200 }}>
              <label htmlFor="slot-doctor">Doctor (optional)</label>
              <select id="slot-doctor" value={slotDoctorId} onChange={(e) => setSlotDoctorId(e.target.value)}>
                <option value="">— none —</option>
                {doctors.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.name} · {d.specialty}
                  </option>
                ))}
              </select>
            </div>
            <div className="field" style={{ marginBottom: 0, minWidth: 220 }}>
              <label htmlFor="slot-start">Date &amp; time</label>
              <input
                id="slot-start"
                type="datetime-local"
                value={slotStart}
                onChange={(e) => setSlotStart(e.target.value)}
                required
              />
            </div>
            <button
              className="btn btn-primary btn-sm"
              id="add-slot-submit"
              type="submit"
              disabled={busy}
              style={{ width: "auto" }}
            >
              Add slot
            </button>
          </div>
        </form>
      </div>

      <div className="card">
        <h3 style={{ marginTop: 0 }}>Book a slot for a patient</h3>
        <form id="book-for-patient-form" onSubmit={handleBook}>
          <div style={{ display: "flex", gap: 12, flexWrap: "wrap", alignItems: "flex-end" }}>
            <div className="field" style={{ marginBottom: 0, minWidth: 200 }}>
              <label htmlFor="book-patient">Patient</label>
              <select id="book-patient" value={bookPatientId} onChange={(e) => setBookPatientId(e.target.value)} required>
                <option value="">— pick a patient —</option>
                {patients.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name} (@{p.username})
                  </option>
                ))}
              </select>
            </div>
            <div className="field" style={{ marginBottom: 0, minWidth: 240 }}>
              <label htmlFor="book-slot">Slot</label>
              <select id="book-slot" value={bookSlotId} onChange={(e) => setBookSlotId(e.target.value)} required>
                <option value="">— pick a free slot —</option>
                {slots?.filter((s) => !isTaken(s)).map((s) => (
                  <option key={s.id} value={s.id}>
                    {formatSlot(s.startTime).raw}
                    {s.doctor ? ` · ${s.doctor.name}` : ""}
                  </option>
                ))}
              </select>
            </div>
            <button
              className="btn btn-primary btn-sm"
              id="book-for-patient-submit"
              type="submit"
              disabled={busy}
              style={{ width: "auto" }}
            >
              Book &amp; confirm
            </button>
          </div>
        </form>
      </div>

      <div className="page-header" style={{ marginTop: 32 }}>
        <h2 id="upcoming-slots-title">Upcoming slots</h2>
      </div>

      {slots?.length === 0 && (
        <div className="card empty-state" id="no-slots-message">
          <div className="icon">🗓️</div>
          No upcoming slots.
        </div>
      )}

      <div style={{ display: "flex", flexDirection: "column", gap: 12 }} id="slots-table">
        {slots?.map((slot) => {
          const dayLoad = slot.doctorDayStatus && DOCTOR_LOAD[slot.doctorDayStatus];
          if (editing?.id === slot.id) {
            return (
              <div className="card" id={`slot-edit-${slot.id}`} key={slot.id}>
                <div style={{ display: "flex", gap: 10, flexWrap: "wrap", alignItems: "flex-end" }}>
                  <div className="field" style={{ marginBottom: 0, minWidth: 180 }}>
                    <label>Doctor</label>
                    <select
                      value={editing.doctorId}
                      onChange={(e) => setEditing((s) => ({ ...s, doctorId: e.target.value }))}
                    >
                      <option value="">— none —</option>
                      {doctors.map((d) => (
                        <option key={d.id} value={d.id}>
                          {d.name}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="field" style={{ marginBottom: 0 }}>
                    <label>Date &amp; time</label>
                    <input
                      type="datetime-local"
                      value={editing.startTime}
                      onChange={(e) => setEditing((s) => ({ ...s, startTime: e.target.value }))}
                    />
                  </div>
                  <button className="btn btn-primary btn-sm" id={`slot-save-${slot.id}`} onClick={saveEdit}
                          style={{ width: "auto" }}>
                    Save
                  </button>
                  <button className="btn btn-secondary btn-sm" onClick={() => setEditing(null)}
                          style={{ width: "auto" }}>
                    Cancel
                  </button>
                </div>
              </div>
            );
          }
          return (
            <div className="card appointment-card" id={`slot-row-${slot.id}`} key={slot.id}>
              <div className="appointment-info">
                <span style={{ fontWeight: 700 }}>{formatSlot(slot.startTime).raw}</span>
                <span style={{ color: "var(--text-muted)", fontSize: "0.85rem" }}>
                  {slot.doctor ? `${slot.doctor.name} · ${slot.doctor.specialty}` : "No doctor assigned"}
                </span>
                <span style={{ display: "flex", gap: 6 }}>
                  <span
                    className={`status-pill status-${slot.booked ? "CONFIRMED" : "REQUESTED"}`}
                    id={`slot-state-${slot.id}`}
                  >
                    {slot.booked ? "Booked" : "Free"}
                  </span>
                  {dayLoad && (
                    <span className={`status-pill status-${dayLoad.pill}`} id={`slot-doctor-load-${slot.id}`}>
                      Doctor {dayLoad.label.toLowerCase()} this day
                    </span>
                  )}
                </span>
              </div>
              <div className="appointment-actions">
                {!slot.booked && (
                  <>
                    <button className="btn btn-secondary btn-sm" id={`slot-edit-btn-${slot.id}`}
                            onClick={() => startEdit(slot)}>
                      Edit
                    </button>
                    <ConfirmButton id={`slot-delete-${slot.id}`} onConfirm={() => removeSlot(slot.id)} />
                  </>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
