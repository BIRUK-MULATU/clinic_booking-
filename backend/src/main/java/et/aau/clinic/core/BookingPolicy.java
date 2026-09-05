package et.aau.clinic.core;

import et.aau.clinic.domain.RejectionReason;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Rule 2: booking eligibility, evaluated as a decision table over three
 * conditions in strict priority order (C1, then C2, then C3).
 *
 * This takes "now" and "slotStart" as plain LocalDateTime rather than a
 * java.time.Clock, so core/ has no dependency on Clock at all - the
 * caller (the service layer) reads Clock.now() once and passes the
 * result in. Tests can then hit the 2-hour boundary exactly just by
 * choosing the two LocalDateTime values, with no Clock stub required
 * inside this class.
 */
public final class BookingPolicy {

    private static final Duration MINIMUM_NOTICE = Duration.ofHours(2);

    private BookingPolicy() {
    }

    public static BookingDecision evaluate(boolean slotFree, boolean noOutstandingBalance,
                                            LocalDateTime now, LocalDateTime slotStart) {
        if (!slotFree) {
            return BookingDecision.reject(RejectionReason.SLOT_UNAVAILABLE);
        }
        if (!noOutstandingBalance) {
            return BookingDecision.reject(RejectionReason.OUTSTANDING_BALANCE);
        }
        boolean sufficientNotice = Duration.between(now, slotStart).compareTo(MINIMUM_NOTICE) >= 0;
        if (!sufficientNotice) {
            return BookingDecision.reject(RejectionReason.INSUFFICIENT_NOTICE);
        }
        return BookingDecision.approve();
    }
}
