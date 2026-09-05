package et.aau.clinic.core;

/**
 * The four events that can move an Appointment through Rule 3's
 * lifecycle. Lives in core/, not domain/, because it is a pure state
 * machine input, not something persisted on the Appointment entity.
 */
public enum AppointmentEvent {
    CONFIRM,
    ATTEND,
    CANCEL,
    MARK_NO_SHOW
}
