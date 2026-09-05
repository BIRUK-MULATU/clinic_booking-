package et.aau.clinic.core;

import et.aau.clinic.domain.RejectionReason;

/**
 * The outcome of BookingPolicy: either approved, or rejected with the
 * one reason that stopped it. A result object rather than an exception,
 * per CLAUDE.md Rule 2 - reasons are easier to assert on and to show
 * in the UI than a stack trace.
 */
public final class BookingDecision {

    private final boolean approved;
    private final RejectionReason reason;

    private BookingDecision(boolean approved, RejectionReason reason) {
        this.approved = approved;
        this.reason = reason;
    }

    public static BookingDecision approve() {
        return new BookingDecision(true, null);
    }

    public static BookingDecision reject(RejectionReason reason) {
        return new BookingDecision(false, reason);
    }

    public boolean isApproved() {
        return approved;
    }

    public RejectionReason getReason() {
        return reason;
    }
}
