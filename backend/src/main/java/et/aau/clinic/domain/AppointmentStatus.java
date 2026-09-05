package et.aau.clinic.domain;

/**
 * The six states of Rule 3's lifecycle (five original plus WAITLISTED,
 * added for hospital-expansion Phase C). ATTENDED, CANCELLED and NO_SHOW
 * are terminal - AppointmentStateMachine rejects every event on them.
 * WAITLISTED is entered by construction, not by transition, exactly like
 * REQUESTED - see AppointmentStateMachine's class comment.
 */
public enum AppointmentStatus {
    REQUESTED,
    CONFIRMED,
    ATTENDED,
    CANCELLED,
    NO_SHOW,
    WAITLISTED
}
