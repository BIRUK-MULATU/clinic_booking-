const API_BASE = "http://localhost:8080/api";

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    ...options,
  });

  const isJson = response.headers.get("content-type")?.includes("application/json");
  const body = isJson ? await response.json() : null;

  if (!response.ok && response.status !== 401) {
    throw new Error(body?.message || `Request failed (${response.status})`);
  }
  return { ok: response.ok, status: response.status, data: body };
}

export const api = {
  session: () => request("/session"),
  login: (username, password) =>
    request("/login", { method: "POST", body: JSON.stringify({ username, password }) }),
  logout: () => request("/logout", { method: "POST" }),
  slots: () => request("/slots"),
  slot: (id) => request(`/slots/${id}`),
  book: (slotId) => request("/bookings", { method: "POST", body: JSON.stringify({ slotId }) }),
  appointments: () => request("/appointments"),
  appointment: (id) => request(`/appointments/${id}`),
  confirm: (id) => request(`/appointments/${id}/confirm`, { method: "POST" }),
  cancel: (id) => request(`/appointments/${id}/cancel`, { method: "POST" }),
  joinWaitlist: (slotId) => request("/waitlist", { method: "POST", body: JSON.stringify({ slotId }) }),
  checkIn: (appointmentId) => request(`/appointments/${appointmentId}/check-in`, { method: "POST" }),

  // Hospital-expansion Phase A/B: doctors and their availability.
  doctors: () => request("/doctors"),
  addDoctor: (doctor) => request("/doctors", { method: "POST", body: JSON.stringify(doctor) }),
  departments: () => request("/departments"),
  addDepartment: (name) => request("/departments", { method: "POST", body: JSON.stringify({ name }) }),
  doctorAvailability: (doctorId) => request(`/doctors/${doctorId}/availability`),
  addAvailabilityRule: (doctorId, rule) =>
    request(`/doctors/${doctorId}/availability`, { method: "POST", body: JSON.stringify(rule) }),
  doctorExceptions: (doctorId) => request(`/doctors/${doctorId}/exceptions`),
  addException: (doctorId, date) =>
    request(`/doctors/${doctorId}/exceptions`, { method: "POST", body: JSON.stringify({ date }) }),

  // Hospital-expansion Phase D: the front-desk queue.
  queue: () => request("/queue"),
  callNext: (entryId) => request(`/queue/${entryId}/call`, { method: "POST" }),
  startConsultation: (entryId) => request(`/queue/${entryId}/start-consultation`, { method: "POST" }),
  completeConsultation: (entryId) => request(`/queue/${entryId}/complete`, { method: "POST" }),

  // Reception's day roster: everyone expected on a given date, checked in or not.
  adminAppointments: (date) => request(`/admin/appointments${date ? `?date=${date}` : ""}`),

  // Hospital-expansion Phase E: the visit record for one appointment. The GET 404s
  // when nothing has been recorded yet, which is a normal state, not an error - so
  // it is caught here and returned as ok:false rather than thrown.
  visitRecord: async (appointmentId) => {
    try {
      return await request(`/appointments/${appointmentId}/visit-record`);
    } catch {
      return { ok: false, status: 404, data: null };
    }
  },
  addVisitRecord: (appointmentId, body) =>
    request(`/appointments/${appointmentId}/visit-record`, { method: "POST", body: JSON.stringify(body) }),
};

export const REJECTION_MESSAGES = {
  SLOT_UNAVAILABLE: "That slot has just been taken. Please pick another one.",
  OUTSTANDING_BALANCE: "You have an outstanding balance and cannot book until it is settled.",
  INSUFFICIENT_NOTICE: "Bookings need at least 2 hours' notice before the slot time.",
};

// Hospital-expansion Phase E: why VisitRecordPolicy refused to record a visit.
export const VISIT_REJECTION_MESSAGES = {
  APPOINTMENT_NOT_ATTENDED: "A visit can only be recorded once the appointment has been attended.",
  VISIT_ALREADY_RECORDED: "This visit has already been recorded.",
  DIAGNOSIS_REQUIRED: "A diagnosis is required.",
  DIAGNOSIS_TOO_LONG: "The diagnosis is too long (500 characters maximum).",
};
