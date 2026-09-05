package et.aau.clinic.core;

import et.aau.clinic.domain.VisitRejection;

/**
 * The outcome of VisitRecordPolicy: either approved, or rejected with
 * the one reason that stopped it. A result object rather than an
 * exception, matching BookingDecision - reasons are easier to assert on
 * and to show in the UI than a stack trace.
 */
public final class VisitRecordDecision {

    private final boolean approved;
    private final VisitRejection reason;

    private VisitRecordDecision(boolean approved, VisitRejection reason) {
        this.approved = approved;
        this.reason = reason;
    }

    public static VisitRecordDecision approve() {
        return new VisitRecordDecision(true, null);
    }

    public static VisitRecordDecision reject(VisitRejection reason) {
        return new VisitRecordDecision(false, reason);
    }

    public boolean isApproved() {
        return approved;
    }

    public VisitRejection getReason() {
        return reason;
    }
}
