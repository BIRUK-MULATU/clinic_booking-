package et.aau.clinic.core;

import et.aau.clinic.domain.AppointmentStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

import static et.aau.clinic.domain.AppointmentStatus.CANCELLED;
import static et.aau.clinic.domain.AppointmentStatus.CONFIRMED;
import static et.aau.clinic.domain.AppointmentStatus.REQUESTED;
import static et.aau.clinic.domain.AppointmentStatus.WAITLISTED;

/**
 * Rule 3: the appointment lifecycle. transition() implements the 6x5
 * table directly - REQUESTED, CONFIRMED and WAITLISTED are the only
 * non-terminal states, so those are the only cases with any outgoing
 * arrow; everything else, including every event on the three terminal
 * states, falls through to the same "invalid" rejection.
 *
 * Hospital-expansion Phase C added WAITLISTED and PROMOTE, extending
 * this table rather than replacing it - the original REQUESTED/CONFIRMED
 * blocks below are unchanged from the original 5-state, 4-event design.
 * An appointment enters WAITLISTED by construction (a booking made when
 * the slot was already taken), the same way REQUESTED is entered by
 * construction rather than by transition - PROMOTE only ever moves an
 * existing WAITLISTED appointment to REQUESTED once its slot frees up.
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
