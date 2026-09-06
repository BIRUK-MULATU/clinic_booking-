package et.aau.clinic.domain;

/**
 * Why VisitRecordPolicy refused to record a visit (hospital-expansion
 * Phase E). Carried on the result object rather than thrown, exactly
 * like RejectionReason for Rule 2 - the service layer and the UI both
 * read it without a try/catch.
 */
public enum VisitRejection {
    APPOINTMENT_NOT_ATTENDED,
    VISIT_ALREADY_RECORDED,
    DIAGNOSIS_REQUIRED,
    DIAGNOSIS_TOO_LONG
}
