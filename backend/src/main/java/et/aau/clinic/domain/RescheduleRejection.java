package et.aau.clinic.domain;

/**
 * Why ReschedulePolicy refused to move an appointment to a new slot
 * (hospital-expansion Rule K). Carried on the result object rather than
 * thrown, the same pattern as RejectionReason (Rule 2) and VisitRejection
 * (Rule E).
 */
public enum RescheduleRejection {
    NOT_RESCHEDULABLE,
    SLOT_UNAVAILABLE,
    INSUFFICIENT_NOTICE
}
