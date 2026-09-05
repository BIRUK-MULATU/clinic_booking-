package et.aau.clinic.core;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * One doctor's recurring weekly availability window (e.g. "Mondays,
 * 09:00-12:00, 30-minute slots"). A plain value type, not a JPA entity -
 * AvailabilityExpander stays container-free the same way BookingPolicy
 * takes plain LocalDateTime rather than a Slot.
 */
public record WeeklyAvailabilityRule(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
                                      int slotDurationMinutes) {
}
