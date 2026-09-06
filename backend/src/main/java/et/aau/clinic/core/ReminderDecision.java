package et.aau.clinic.core;

import et.aau.clinic.domain.ReminderSkipReason;

/**
 * The outcome of ReminderPolicy: either "send a reminder now", or
 * "skip" with the one reason that stopped it. A result object rather
 * than a boolean or an exception, matching BookingDecision and
 * VisitRecordDecision - the reason is easy to assert on and to display.
 */
public final class ReminderDecision {

    private final boolean due;
    private final ReminderSkipReason reason;

    private ReminderDecision(boolean due, ReminderSkipReason reason) {
        this.due = due;
        this.reason = reason;
    }

    public static ReminderDecision send() {
        return new ReminderDecision(true, null);
    }

    public static ReminderDecision skip(ReminderSkipReason reason) {
        return new ReminderDecision(false, reason);
    }

    public boolean isDue() {
        return due;
    }

    public ReminderSkipReason getReason() {
        return reason;
    }
}
