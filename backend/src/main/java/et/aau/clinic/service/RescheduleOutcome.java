package et.aau.clinic.service;

import et.aau.clinic.core.RescheduleDecision;
import et.aau.clinic.domain.Appointment;

/**
 * The service-layer result of a reschedule attempt (Rule K): the core
 * decision, plus the moved appointment - null when the decision was a
 * rejection. Mirrors BookingOutcome.
 */
public record RescheduleOutcome(RescheduleDecision decision, Appointment appointment) {
}
