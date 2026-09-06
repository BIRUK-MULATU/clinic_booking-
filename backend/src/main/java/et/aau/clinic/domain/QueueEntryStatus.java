package et.aau.clinic.domain;

/**
 * The four states of a patient's day-of-visit queue entry (hospital-
 * expansion Phase D). Deliberately a separate small machine from
 * AppointmentStatus - see the Phase D planning discussion: a QueueEntry
 * lives for hours, an Appointment lives for days/weeks, and cramming
 * both into one enum would mean one state machine answering two
 * unrelated questions. DONE is terminal.
 */
public enum QueueEntryStatus {
    WAITING,
    CALLED,
    IN_CONSULTATION,
    DONE
}
