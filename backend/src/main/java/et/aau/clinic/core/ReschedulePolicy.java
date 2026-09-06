package et.aau.clinic.core;

import et.aau.clinic.domain.AppointmentStatus;
import et.aau.clinic.domain.RescheduleRejection;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Rule I (hospital-expansion): moving an appointment to a different
 * slot, evaluated as a decision table over three conditions in strict
 * priority order - the same shape as BookingPolicy (Rule 2).
 *
 *   C1 - the appointment is in a reschedulable state (REQUESTED or
 *        CONFIRMED). ATTENDED / CANCELLED / NO_SHOW / OFFER_EXPIRED are
 *        done, and WAITLISTED has no slot of its own to move yet.
 *   C2 - the new slot is free.
 *   C3 - the new slot starts at least 2 hours from now - the same
 *        notice Rule 2's C3 requires for a fresh booking.
 *
 * Actions:
 *   C1 false          -> reject NOT_RESCHEDULABLE
 *   C1 true, C2 false -> reject SLOT_UNAVAILABLE   (regardless of C3)
 *   C1, C2 true, C3 F -> reject INSUFFICIENT_NOTICE
 *   all true          -> approve
 *
 * Pure: the caller turns "is the new slot free" into a repository
 * lookup and reads the Clock, then passes plain values in. The 2-hour
 * threshold is reused from BookingPolicy so the boundary test reads the
 * same way: a new slot exactly 2h00m out is allowed, 1h59m is not.
 */
public final class ReschedulePolicy {

    private static final Duration MINIMUM_NOTICE = Duration.ofHours(2);

    private ReschedulePolicy() {
    }

    public static RescheduleDecision evaluate(AppointmentStatus currentStatus, boolean newSlotFree,
                                              LocalDateTime now, LocalDateTime newSlotStart) {
        if (currentStatus != AppointmentStatus.REQUESTED && currentStatus != AppointmentStatus.CONFIRMED) {
            return RescheduleDecision.reject(RescheduleRejection.NOT_RESCHEDULABLE);
        }
        if (!newSlotFree) {
            return RescheduleDecision.reject(RescheduleRejection.SLOT_UNAVAILABLE);
        }
        boolean sufficientNotice = Duration.between(now, newSlotStart).compareTo(MINIMUM_NOTICE) >= 0;
        if (!sufficientNotice) {
            return RescheduleDecision.reject(RescheduleRejection.INSUFFICIENT_NOTICE);
        }
        return RescheduleDecision.approve();
    }
}
