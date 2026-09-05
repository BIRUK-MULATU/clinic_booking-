package et.aau.clinic.core;

import et.aau.clinic.domain.QueueEntryStatus;

import static et.aau.clinic.domain.QueueEntryStatus.CALLED;
import static et.aau.clinic.domain.QueueEntryStatus.DONE;
import static et.aau.clinic.domain.QueueEntryStatus.IN_CONSULTATION;
import static et.aau.clinic.domain.QueueEntryStatus.WAITING;

/**
 * Hospital-expansion Phase D: the day-of-visit queue lifecycle. A
 * separate, deliberately small state machine - 4 states x 3 events = 12
 * pairs, 3 valid, 9 invalid - not folded into AppointmentStateMachine.
 * QueueEntry enters WAITING by construction (at check-in), the same way
 * Appointment enters REQUESTED/WAITLISTED by construction rather than
 * by transition.
 */
public final class QueueStateMachine {

    private QueueStateMachine() {
    }

    public static QueueEntryStatus transition(QueueEntryStatus current, QueueEvent event) {
        if (current == WAITING) {
            if (event == QueueEvent.CALL) {
                return CALLED;
            }
        }
        if (current == CALLED) {
            if (event == QueueEvent.START_CONSULTATION) {
                return IN_CONSULTATION;
            }
        }
        if (current == IN_CONSULTATION) {
            if (event == QueueEvent.COMPLETE) {
                return DONE;
            }
        }
        throw new IllegalStateException(
                "Cannot apply event " + event + " to queue entry in state " + current);
    }
}
