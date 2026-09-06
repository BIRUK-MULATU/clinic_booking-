package et.aau.clinic.domain;

/**
 * Why ReminderPolicy decided not to send a reminder for an appointment
 * (hospital-expansion: Rule H). Carried on the result object rather than
 * thrown, exactly like RejectionReason (Rule 2) and VisitRejection
 * (Rule E) - the scheduled job reads it without a try/catch, and it
 * doubles as the label shown on the my-appointments page.
 */
public enum ReminderSkipReason {
    NOT_CONFIRMED,
    ALREADY_REMINDED,
    SLOT_ALREADY_STARTED,
    NOT_YET_DUE
}
