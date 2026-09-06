package et.aau.clinic.core;

import et.aau.clinic.domain.AppointmentStatus;
import et.aau.clinic.domain.ReminderSkipReason;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Rule H (hospital-expansion): whether a reminder should be sent to the
 * patient now, evaluated as a decision table over four conditions in
 * strict priority order - the same shape as BookingPolicy (Rule 2) and
 * VisitRecordPolicy (Rule E).
 *
 *   C1 - the appointment is CONFIRMED (a REQUESTED one is not yet a
 *        commitment; ATTENDED / CANCELLED / NO_SHOW / WAITLISTED have
 *        nothing to remind about)
 *   C2 - a reminder has not already been sent
 *   C3 - the slot has not already started
 *   C4 - the slot starts within the 24-hour reminder window
 *
 * Actions:
 *   C1 false            -> skip NOT_CONFIRMED
 *   C1 true, C2 false   -> skip ALREADY_REMINDED
 *   C1, C2 true, C3 F   -> skip SLOT_ALREADY_STARTED
 *   C1..C3 true, C4 F   -> skip NOT_YET_DUE
 *   all true            -> send
 *
 * Pure: it takes the status, the two timestamps and the
 * already-reminded boolean, so the service layer turns "has a reminder
 * been sent" into a field read and passes the answer in. No Spring, no
 * Clock - the caller supplies `now` from the injected Clock.
 *
 * BVA target: the 24-hour window is inclusive at the boundary - a slot
 * exactly 24h00m away IS reminded. This is deliberately the opposite
 * inclusivity from Rule 3b's late-cancellation guard ("less than 24
 * hours" - exactly 24h00m is free), so the two boundary tests read as a
 * matched pair. Boundaries to test: 24h01m (not yet due), exactly
 * 24h00m (send), 0h01m (send), and slot already started (skip).
 */
public final class ReminderPolicy {

    static final Duration REMINDER_WINDOW = Duration.ofHours(24);

    private ReminderPolicy() {
    }

    public static ReminderDecision decide(AppointmentStatus status, LocalDateTime now,
                                          LocalDateTime slotStart, boolean alreadyReminded) {
        if (status != AppointmentStatus.CONFIRMED) {
            return ReminderDecision.skip(ReminderSkipReason.NOT_CONFIRMED);
        }
        if (alreadyReminded) {
            return ReminderDecision.skip(ReminderSkipReason.ALREADY_REMINDED);
        }
        Duration untilSlot = Duration.between(now, slotStart);
        if (untilSlot.isZero() || untilSlot.isNegative()) {
            return ReminderDecision.skip(ReminderSkipReason.SLOT_ALREADY_STARTED);
        }
        if (untilSlot.compareTo(REMINDER_WINDOW) > 0) {
            return ReminderDecision.skip(ReminderSkipReason.NOT_YET_DUE);
        }
        return ReminderDecision.send();
    }
}
