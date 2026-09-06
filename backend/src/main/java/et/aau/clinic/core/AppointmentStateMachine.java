package et.aau.clinic.core;

import et.aau.clinic.domain.AppointmentStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

import static et.aau.clinic.domain.AppointmentStatus.CANCELLED;
import static et.aau.clinic.domain.AppointmentStatus.CONFIRMED;
import static et.aau.clinic.domain.AppointmentStatus.OFFER_EXPIRED;
import static et.aau.clinic.domain.AppointmentStatus.REQUESTED;
import static et.aau.clinic.domain.AppointmentStatus.WAITLISTED;

/**
 * Rule 3: the appointment lifecycle. transition() implements the 7x7
 * table directly - REQUESTED, CONFIRMED and WAITLISTED are the only
 * non-terminal states, so those are the only cases with any outgoing
 * arrow; everything else, including every event on the four terminal
 * states, falls through to the same "invalid" rejection.
 *
 * The table has grown twice by extension, never by rewrite - the
 * original REQUESTED/CONFIRMED blocks below are unchanged from the
 * original 5-state, 4-event design:
 *   - Phase C added WAITLISTED and PROMOTE.
 *   - Rule I added RESCHEDULE: a slot move that keeps the appointment in
 *     its current state, so REQUESTED and CONFIRMED each get a self-loop.
 *   - Rule J added OFFER_EXPIRED (a terminal state) and EXPIRE_OFFER: a
 *     promoted waitlist offer that the patient did not accept in the
 *     2-hour window lapses from REQUESTED to OFFER_EXPIRED.
 * REQUESTED and WAITLISTED are entered by construction, not by
 * transition (a fresh booking, or one made when the slot was taken).
 */
public final class AppointmentStateMachine {

    private static final Duration LATE_CANCELLATION_WINDOW = Duration.ofHours(24);
    private static final BigDecimal LATE_CANCELLATION_RATE = new BigDecimal("0.5");

    private AppointmentStateMachine() {
    }

    public static AppointmentStatus transition(AppointmentStatus current, AppointmentEvent event) {
        if (current == REQUESTED) {
            if (event == AppointmentEvent.CONFIRM) {
                return CONFIRMED;
            }
            if (event == AppointmentEvent.CANCEL) {
                return CANCELLED;
            }
            if (event == AppointmentEvent.RESCHEDULE) {
                return REQUESTED;
            }
            if (event == AppointmentEvent.EXPIRE_OFFER) {
                return OFFER_EXPIRED;
            }
        }
        if (current == CONFIRMED) {
            if (event == AppointmentEvent.ATTEND) {
                return AppointmentStatus.ATTENDED;
            }
            if (event == AppointmentEvent.CANCEL) {
                return CANCELLED;
            }
            if (event == AppointmentEvent.MARK_NO_SHOW) {
                return AppointmentStatus.NO_SHOW;
            }
            if (event == AppointmentEvent.RESCHEDULE) {
                return CONFIRMED;
            }
        }
        if (current == WAITLISTED) {
            if (event == AppointmentEvent.PROMOTE) {
                return REQUESTED;
            }
            if (event == AppointmentEvent.CANCEL) {
                return CANCELLED;
            }
        }
        throw new IllegalStateException(
                "Cannot apply event " + event + " to appointment in state " + current);
    }

    /**
     * Rule 3b: cancelling a CONFIRMED appointment less than 24 hours
     * before its slot start charges 50% of the consultation fee.
     * Only meaningful when the appointment being cancelled was
     * CONFIRMED - the caller does not invoke this for a REQUESTED
     * cancellation, which is always free.
     */
    public static BigDecimal lateCancellationFee(LocalDateTime now, LocalDateTime slotStart,
                                                  BigDecimal consultationFee) {
        boolean withinLateWindow = Duration.between(now, slotStart).compareTo(LATE_CANCELLATION_WINDOW) < 0;
        return withinLateWindow ? consultationFee.multiply(LATE_CANCELLATION_RATE) : BigDecimal.ZERO;
    }
}
