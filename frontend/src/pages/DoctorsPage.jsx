import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { api, DOCTOR_LOAD } from "../api";
import { formatAvailability, readImageAsDataUrl } from "../utils";
import ConfirmButton from "../components/ConfirmButton";

export default function DoctorsPage() {
  const [doctors, setDoctors] = useState(null);
  const [departments, setDepartments] = useState(null);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const [departmentName, setDepartmentName] = useState("");

  const [doctorName, setDoctorName] = useState("");
  const [specialty, setSpecialty] = useState("");
  const [departmentId, setDepartmentId] = useState("");
  const [photo, setPhoto] = useState("");
  const [newLimit, setNewLimit] = useState(8);

  const [limitDrafts, setLimitDrafts] = useState({});
  const [editingDoctor, setEditingDoctor] = useState(null); // { id, name, specialty, departmentId }
  const [deptDrafts, setDeptDrafts] = useState({});

  const photoInputs = useRef({});

  function load() {
    api
      .doctors()
      .then(({ ok, data }) => {
        if (!ok) {
          setError("Could not load doctors.");
          return;
        }
        setDoctors(data);
        setLimitDrafts(Object.fromEntries(data.map((d) => [d.id, d.dailyPatientLimit])));
      })
      .catch(() => setError("Could not reach the server."));
    api
      .departments()
      .then(({ ok, data }) => {
        if (!ok) return;
        setDepartments(data);
        setDeptDrafts(Object.fromEntries(data.map((d) => [d.id, d.name])));
        setDepartmentId((current) => current || data[0]?.id || "");
      })
      .catch(() => setError("Could not reach the server."));
  }

  useEffect(load, []);

  async function pickPhoto(file, onReady) {
    if (!file) return;
    try {
      onReady(await readImageAsDataUrl(file));
      setError("");
    } catch (err) {
      setError(err.message || "Could not read that image.");
    }
  }

  async function handleAddDepartment(e) {
    e.preventDefault();
    if (!departmentName.trim()) return;
    setSubmitting(true);
    setError("");
    try {
      const { ok } = await api.addDepartment(departmentName.trim());
      if (!ok) {
        setError("Could not add the department.");
        return;
      }
      setDepartmentName("");
      load();
    } catch {
      setError("Could not reach the server.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleAddDoctor(e) {
    e.preventDefault();
    if (!doctorName.trim() || !specialty.trim() || !departmentId) return;
    setSubmitting(true);
    setError("");
    try {
      const { ok, data } = await api.addDoctor({
        name: doctorName.trim(),
        specialty: specialty.trim(),
        departmentId: Number(departmentId),
        photo: photo || null,
        dailyPatientLimit: Number(newLimit) || null,
      });
      if (!ok) {
        setError(data?.message || "Could not add the doctor.");
        return;
      }
      setDoctorName("");
      setSpecialty("");
      setPhoto("");
      setNewLimit(8);
      load();
    } catch (err) {
      setError(err.message || "Could not reach the server.");
    } finally {
      setSubmitting(false);
    }
  }

  async function changePhoto(doctorId, dataUrl) {
    setError("");
    try {
      const { ok, data } = await api.setDoctorPhoto(doctorId, dataUrl);
      if (!ok) {
        setError(data?.message || "Could not update the photo.");
        return;
      }
      load();
    } catch (err) {
      setError(err.message || "Could not reach the server.");
    }
  }

  async function saveDoctorEdit() {
    setError("");
    try {
      const { ok, data } = await api.updateDoctor(editingDoctor.id, {
        name: editingDoctor.name,
        specialty: editingDoctor.specialty,
        departmentId: Number(editingDoctor.departmentId),
      });
      if (!ok) {
        setError(data?.message || "Could not update the doctor.");
        return;
      }
      setEditingDoctor(null);
      load();
    } catch (err) {
      setError(err.message || "Could not reach the server.");
    }
  }

  async function removeDoctor(id) {
    setError("");
    try {
      const { ok, data } = await api.deleteDoctor(id);
      if (!ok) {
        setError(data?.message || "Could not delete the doctor.");
        return;
      }
      load();
    } catch (err) {
      setError(err.message || "Could not reach the server.");
    }
  }

  async function saveDeptName(id) {
    setError("");
    try {
      const { ok, data } = await api.updateDepartment(id, deptDrafts[id]);
      if (!ok) {
        setError(data?.message || "Could not rename the department.");
        return;
      }
      load();
    } catch (err) {
      setError(err.message || "Could not reach the server.");
    }
  }

  async function removeDept(id) {
    setError("");
    try {
      const { ok, data } = await api.deleteDepartment(id);
      if (!ok) {
        setError(data?.message || "Could not delete the department.");
        return;
      }
      load();
    } catch (err) {
      setError(err.message || "Could not reach the server.");
    }
  }

  async function saveLimit(doctorId) {
    setError("");
    try {
      const { ok, data } = await api.setDoctorLimit(doctorId, Number(limitDrafts[doctorId]));
      if (!ok) {
        setError(data?.message || "Could not update the limit.");
        return;
      }
      load();
    } catch (err) {
      setError(err.message || "Could not reach the server.");
    }
  }

  return (
    <div className="page" id="doctors-page">
      <div className="page-header">
        <h1 id="page-title">Doctors</h1>
        <p>Each doctor's specialty, weekly availability, photo, and how full their day is today.</p>
      </div>

      {error && <p className="alert alert-error" id="doctors-error">{error}</p>}

      {doctors?.length === 0 && (
        <div className="card empty-state" id="no-doctors-message">
          <div className="icon">🩺</div>
          No doctors yet.
        </div>
      )}

      <div style={{ display: "flex", flexDirection: "column", gap: 12, marginBottom: 16 }} id="doctors-table">
        {doctors?.map((doctor) => {
          const loadInfo = doctor.todayLoad && DOCTOR_LOAD[doctor.todayLoad.status];
          if (editingDoctor?.id === doctor.id) {
            return (
              <div className="card" id={`doctor-edit-${doctor.id}`} key={doctor.id}>
                <div style={{ display: "flex", gap: 10, flexWrap: "wrap", alignItems: "flex-end" }}>
                  <div className="field" style={{ marginBottom: 0 }}>
                    <label>Name</label>
                    <input
                      value={editingDoctor.name}
                      onChange={(e) => setEditingDoctor((s) => ({ ...s, name: e.target.value }))}
                    />
                  </div>
                  <div className="field" style={{ marginBottom: 0 }}>
                    <label>Specialty</label>
                    <input
                      value={editingDoctor.specialty}
                      onChange={(e) => setEditingDoctor((s) => ({ ...s, specialty: e.target.value }))}
                    />
                  </div>
                  <div className="field" style={{ marginBottom: 0 }}>
                    <label>Department</label>
                    <select
                      value={editingDoctor.departmentId}
                      onChange={(e) => setEditingDoctor((s) => ({ ...s, departmentId: e.target.value }))}
                    >
                      {departments?.map((d) => (
                        <option key={d.id} value={d.id}>
                          {d.name}
                        </option>
                      ))}
                    </select>
                  </div>
                  <button className="btn btn-primary btn-sm" id={`doctor-save-${doctor.id}`} onClick={saveDoctorEdit}
                          style={{ width: "auto" }}>
                    Save
                  </button>
                  <button className="btn btn-secondary btn-sm" onClick={() => setEditingDoctor(null)}
                          style={{ width: "auto" }}>
                    Cancel
                  </button>
                </div>
              </div>
            );
          }
          return (
            <div className="card appointment-card" id={`doctor-row-${doctor.id}`} key={doctor.id}>
              <div className="appointment-info" style={{ flexDirection: "row", alignItems: "center", gap: 14 }}>
                {doctor.photo ? (
                  <img className="doctor-photo" id={`doctor-photo-${doctor.id}`} src={doctor.photo} alt={doctor.name} />
                ) : (
                  <span className="doctor-photo doctor-photo--placeholder" id={`doctor-photo-${doctor.id}`}>
                    🩺
                  </span>
                )}
                <span style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                  <span style={{ fontWeight: 700 }}>{doctor.name}</span>
                  <span style={{ color: "var(--text-muted)", fontSize: "0.85rem" }}>
                    {doctor.specialty} · {doctor.departmentName}
                  </span>
                  <span
                    style={{ color: "var(--text-muted)", fontSize: "0.8rem" }}
                    id={`doctor-availability-${doctor.id}`}
                  >
                    {formatAvailability(doctor.availability)}
                  </span>
                  {doctor.todayLoad && (
                    <span style={{ display: "flex", alignItems: "center", gap: 8, marginTop: 2 }}>
                      <span className={`status-pill status-${loadInfo.pill}`} id={`doctor-load-${doctor.id}`}>
                        {loadInfo.label}
                      </span>
                      <span style={{ color: "var(--text-muted)", fontSize: "0.8rem" }}>
                        {doctor.todayLoad.scheduled} of {doctor.todayLoad.dailyLimit} patients today ·{" "}
                        {doctor.todayLoad.remaining} left
                      </span>
                    </span>
                  )}
                </span>
              </div>
              <div className="appointment-actions" style={{ flexWrap: "wrap" }}>
                <span style={{ display: "flex", alignItems: "center", gap: 6 }}>
                  <label htmlFor={`doctor-limit-${doctor.id}`} style={{ fontSize: "0.8rem", color: "var(--text-muted)" }}>
                    Daily limit
                  </label>
                  <input
                    id={`doctor-limit-${doctor.id}`}
                    type="number"
                    min="1"
                    value={limitDrafts[doctor.id] ?? ""}
                    onChange={(e) => setLimitDrafts((d) => ({ ...d, [doctor.id]: e.target.value }))}
                    style={{ width: 64, padding: "6px 8px", border: "1px solid var(--border)", borderRadius: 8 }}
                  />
                  <button
                    className="btn btn-secondary btn-sm"
                    id={`save-limit-${doctor.id}`}
                    disabled={Number(limitDrafts[doctor.id]) === doctor.dailyPatientLimit}
                    onClick={() => saveLimit(doctor.id)}
                  >
                    Save
                  </button>
                </span>
                <input
                  type="file"
                  accept="image/*"
                  hidden
                  ref={(el) => {
                    photoInputs.current[doctor.id] = el;
                  }}
                  id={`doctor-photo-input-${doctor.id}`}
                  onChange={(e) => pickPhoto(e.target.files[0], (url) => changePhoto(doctor.id, url))}
                />
                <button
                  className="btn btn-secondary btn-sm"
                  id={`change-photo-${doctor.id}`}
                  onClick={() => photoInputs.current[doctor.id]?.click()}
                >
                  {doctor.photo ? "Change photo" : "Add photo"}
                </button>
                <Link
                  className="btn btn-secondary btn-sm"
                  id={`manage-availability-link-${doctor.id}`}
                  to={`/doctors/${doctor.id}/availability`}
                >
                  Manage Availability
                </Link>
                <button
                  className="btn btn-secondary btn-sm"
                  id={`doctor-edit-btn-${doctor.id}`}
                  onClick={() =>
                    setEditingDoctor({
                      id: doctor.id,
                      name: doctor.name,
                      specialty: doctor.specialty,
                      departmentId: String(departments?.find((d) => d.name === doctor.departmentName)?.id ?? ""),
                    })
                  }
                >
                  Edit
                </button>
                <ConfirmButton id={`doctor-delete-${doctor.id}`} onConfirm={() => removeDoctor(doctor.id)} />
              </div>
            </div>
          );
        })}
      </div>

      <div className="card">
        <h3 style={{ marginTop: 0 }}>Add a doctor</h3>
        <form id="add-doctor-form" onSubmit={handleAddDoctor}>
          <div style={{ display: "flex", gap: 10, flexWrap: "wrap", alignItems: "flex-end" }}>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="doctor-name">Name</label>
              <input
                id="doctor-name"
                placeholder="Dr. Ada Lovelace"
                value={doctorName}
                onChange={(e) => setDoctorName(e.target.value)}
                required
              />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="doctor-specialty">Specialty</label>
              <input
                id="doctor-specialty"
                placeholder="Dermatologist"
                value={specialty}
                onChange={(e) => setSpecialty(e.target.value)}
                required
              />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="doctor-department">Department</label>
              <select id="doctor-department" value={departmentId} onChange={(e) => setDepartmentId(e.target.value)}>
                {departments?.map((department) => (
                  <option key={department.id} value={department.id}>
                    {department.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="field" style={{ marginBottom: 0, width: 130 }}>
              <label htmlFor="doctor-limit">Daily limit</label>
              <input
                id="doctor-limit"
                type="number"
                min="1"
                value={newLimit}
                onChange={(e) => setNewLimit(e.target.value)}
              />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="doctor-photo-input">Photo</label>
              <input
                id="doctor-photo-input"
                type="file"
                accept="image/*"
                onChange={(e) => pickPhoto(e.target.files[0], setPhoto)}
              />
            </div>
            {photo && <img className="doctor-photo" src={photo} alt="preview" id="doctor-photo-preview" />}
            <button
              className="btn btn-primary btn-sm"
              id="add-doctor-submit"
              type="submit"
              disabled={submitting || !departmentId}
              style={{ width: "auto" }}
            >
              Add doctor
            </button>
          </div>
          {!departments?.length && (
            <p style={{ color: "var(--text-muted)", fontSize: "0.85rem", marginTop: 8 }}>
              Add a department below before adding a doctor.
            </p>
          )}
        </form>
      </div>

      {departments?.length > 0 && (
        <div className="card" id="departments-table">
          <h3 style={{ marginTop: 0 }}>Departments</h3>
          <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
            {departments.map((dept) => (
              <div
                key={dept.id}
                id={`department-row-${dept.id}`}
                style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}
              >
                <input
                  value={deptDrafts[dept.id] ?? ""}
                  onChange={(e) => setDeptDrafts((d) => ({ ...d, [dept.id]: e.target.value }))}
                  style={{ padding: "6px 10px", border: "1px solid var(--border)", borderRadius: 8, minWidth: 180 }}
                />
                <button
                  className="btn btn-secondary btn-sm"
                  id={`department-save-${dept.id}`}
                  disabled={deptDrafts[dept.id] === dept.name}
                  onClick={() => saveDeptName(dept.id)}
                >
                  Rename
                </button>
                <ConfirmButton id={`department-delete-${dept.id}`} onConfirm={() => removeDept(dept.id)} />
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="card">
        <h3 style={{ marginTop: 0 }}>Add a department</h3>
        <form id="add-department-form" onSubmit={handleAddDepartment}>
          <div style={{ display: "flex", gap: 10, alignItems: "flex-end" }}>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="department-name">Name</label>
              <input
                id="department-name"
                placeholder="Dermatology"
                value={departmentName}
                onChange={(e) => setDepartmentName(e.target.value)}
                required
              />
            </div>
            <button
              className="btn btn-secondary btn-sm"
              id="add-department-submit"
              type="submit"
              disabled={submitting}
              style={{ width: "auto" }}
            >
              Add department
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
