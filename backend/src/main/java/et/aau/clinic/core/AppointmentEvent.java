package et.aau.clinic.core;

/**
 * The seven events that can move an Appointment through Rule 3's
 * lifecycle: four original, plus PROMOTE (hospital-expansion Phase C),
 * RESCHEDULE (Rule K - move an appointment to a different slot, keeping
 * its state) and EXPIRE_OFFER (Rule L - a promoted waitlist offer the
 * patient let lapse). Lives in core/, not domain/, because it is a pure
 * state machine input, not something persisted on the Appointment entity.
 */
public enum AppointmentEvent {
    CONFIRM,
    ATTEND,
    CANCEL,
    MARK_NO_SHOW,
    PROMOTE,
    RESCHEDULE,
    EXPIRE_OFFER
}
