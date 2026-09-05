package et.aau.clinic.core;

/**
 * The five events that can move an Appointment through Rule 3's
 * lifecycle (four original plus PROMOTE, added for hospital-expansion
 * Phase C). Lives in core/, not domain/, because it is a pure state
 * machine input, not something persisted on the Appointment entity.
 */
public enum AppointmentEvent {
    CONFIRM,
    ATTEND,
    CANCEL,
    MARK_NO_SHOW,
    PROMOTE
}
