package et.aau.clinic.core;

import et.aau.clinic.domain.RescheduleRejection;

/**
 * The outcome of ReschedulePolicy: either approved, or rejected with the
 * one reason that stopped it. A result object rather than an exception,
 * matching BookingDecision and VisitRecordDecision.
 */
public final class RescheduleDecision {

    private final boolean approved;
    private final RescheduleRejection reason;

    private RescheduleDecision(boolean approved, RescheduleRejection reason) {
        this.approved = approved;
        this.reason = reason;
    }

    public static RescheduleDecision approve() {
        return new RescheduleDecision(true, null);
    }

    public static RescheduleDecision reject(RescheduleRejection reason) {
        return new RescheduleDecision(false, reason);
    }

    public boolean isApproved() {
        return approved;
    }

    public RescheduleRejection getReason() {
        return reason;
    }
}
