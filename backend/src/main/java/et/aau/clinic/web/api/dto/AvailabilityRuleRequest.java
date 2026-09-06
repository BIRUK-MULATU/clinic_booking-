package et.aau.clinic.web.api.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record AvailabilityRuleRequest(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
                                       int slotDurationMinutes) {
}
