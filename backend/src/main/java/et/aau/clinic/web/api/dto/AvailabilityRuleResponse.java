package et.aau.clinic.web.api.dto;

import et.aau.clinic.domain.DoctorAvailability;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record AvailabilityRuleResponse(Long id, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
                                        int slotDurationMinutes) {

    public static AvailabilityRuleResponse from(DoctorAvailability rule) {
        return new AvailabilityRuleResponse(rule.getId(), rule.getDayOfWeek(), rule.getStartTime(),
                rule.getEndTime(), rule.getSlotDurationMinutes());
    }
}
