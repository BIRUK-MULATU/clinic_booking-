package et.aau.clinic.core;

/**
 * The three events that move a QueueEntry through its lifecycle
 * (hospital-expansion Phase D). Lives in core/, not domain/, mirroring
 * AppointmentEvent - a pure state machine input, not persisted.
 */
public enum QueueEvent {
    CALL,
    START_CONSULTATION,
    COMPLETE
}
