package et.aau.clinic.service;

import et.aau.clinic.core.BookingDecision;
import et.aau.clinic.domain.Appointment;

/**
 * The service-layer result of a booking attempt: the core decision,
 * plus the Appointment that was actually created - null when the
 * decision was a rejection, since there is nothing to show for it.
 */
public record BookingOutcome(BookingDecision decision, Appointment appointment) {
}
