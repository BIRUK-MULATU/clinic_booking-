package et.aau.clinic.domain;

/**
 * The five states of Rule 3's lifecycle. ATTENDED, CANCELLED and NO_SHOW
 * are terminal - AppointmentStateMachine rejects every event on them.
 */
public enum AppointmentStatus {
    REQUESTED,
    CONFIRMED,
    ATTENDED,
    CANCELLED,
    NO_SHOW
}
