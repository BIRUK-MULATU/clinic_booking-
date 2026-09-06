package et.aau.clinic.core;

import et.aau.clinic.domain.VisitRejection;

/**
 * Rule E (hospital-expansion Phase E): whether a visit record may be
 * created for an appointment, evaluated as a decision table over three
 * conditions in strict priority order (C1, then C2, then C3) - the same
 * shape as BookingPolicy (Rule 2).
 *
 *   C1 - the appointment has been ATTENDED
 *   C2 - no visit record exists for it yet (it is @OneToOne)
 *   C3 - the diagnosis, trimmed, is between 1 and 500 characters
 *
 * Actions:
 *   C1 false                     -> reject APPOINTMENT_NOT_ATTENDED
 *   C1 true, C2 false            -> reject VISIT_ALREADY_RECORDED
 *   C1, C2 true, diagnosis blank -> reject DIAGNOSIS_REQUIRED
 *   C1, C2 true, diagnosis > 500 -> reject DIAGNOSIS_TOO_LONG
 *   all true                     -> approve
 *
 * Pure: it takes the two booleans and the raw diagnosis string, so the
 * service layer turns "has this appointment been attended / does a
 * record already exist" into repository lookups and passes the answers
 * in. No Spring, no Clock.
 *
 * The 500-character maximum is Rule E's boundary-value target: the
 * boundaries to test are 0 (blank), 1, 500 and 501.
 */
public final class VisitRecordPolicy {

    static final int MAX_DIAGNOSIS_LENGTH = 500;

    private VisitRecordPolicy() {
    }

    public static VisitRecordDecision evaluate(boolean appointmentAttended, boolean noExistingRecord,
                                               String diagnosis) {
        if (!appointmentAttended) {
            return VisitRecordDecision.reject(VisitRejection.APPOINTMENT_NOT_ATTENDED);
        }
        if (!noExistingRecord) {
            return VisitRecordDecision.reject(VisitRejection.VISIT_ALREADY_RECORDED);
        }
        String trimmed = diagnosis == null ? "" : diagnosis.trim();
        if (trimmed.isEmpty()) {
            return VisitRecordDecision.reject(VisitRejection.DIAGNOSIS_REQUIRED);
        }
        if (trimmed.length() > MAX_DIAGNOSIS_LENGTH) {
            return VisitRecordDecision.reject(VisitRejection.DIAGNOSIS_TOO_LONG);
        }
        return VisitRecordDecision.approve();
    }
}
