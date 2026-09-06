package et.aau.clinic.domain;

/**
 * The seven states of Rule 3's lifecycle: five original, plus WAITLISTED
 * (hospital-expansion Phase C) and OFFER_EXPIRED (hospital-expansion
 * Rule L - a waitlist promotion the patient did not accept in time).
 * ATTENDED, CANCELLED, NO_SHOW and OFFER_EXPIRED are terminal -
 * AppointmentStateMachine rejects every event on them. REQUESTED and
 * WAITLISTED are entered by construction, not by transition - see
 * AppointmentStateMachine's class comment.
 */
public enum AppointmentStatus {
    REQUESTED,
    CONFIRMED,
    ATTENDED,
    CANCELLED,
    NO_SHOW,
    WAITLISTED,
    OFFER_EXPIRED
}
