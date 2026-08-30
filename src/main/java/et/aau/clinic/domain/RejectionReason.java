package et.aau.clinic.domain;

/**
 * Why BookingPolicy refused a request (Rule 2). Carried on the result
 * object rather than thrown, so both the service layer and the UI can
 * read it without a try/catch.
 */
public enum RejectionReason {
    SLOT_UNAVAILABLE,
    OUTSTANDING_BALANCE,
    INSUFFICIENT_NOTICE
}
