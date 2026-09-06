package et.aau.clinic.core;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Turns a doctor's weekly recurring rules into concrete candidate slot
 * start times over a fixed rolling horizon. Pure and container-free, like
 * FeeCalculator and BookingPolicy - the caller (a future AvailabilityService)
 * is the only place that touches Spring or JPA.
 *
 * Per-day decision, in priority order (same shape as Rule 2's C1/C2/C3):
 * C1 = the day's day-of-week matches a configured rule; C2 = the date
 * falls on an exception (leave/holiday). C2 true overrides C1 regardless -
 * an exception cancels an otherwise-scheduled day.
 */
public final class AvailabilityExpander {

    /** Rolling horizon: generate over [from, from + HORIZON_DAYS). */
    public static final int HORIZON_DAYS = 14;

    private AvailabilityExpander() {
    }

    public static List<LocalDateTime> expand(List<WeeklyAvailabilityRule> rules, Set<LocalDate> exceptionDates,
                                               LocalDate from) {
        List<LocalDateTime> candidates = new ArrayList<>();
        for (int offset = 0; offset < HORIZON_DAYS; offset++) {
            candidates.addAll(slotsForDay(from.plusDays(offset), rules, exceptionDates));
        }
        return candidates;
    }

    private static List<LocalDateTime> slotsForDay(LocalDate day, List<WeeklyAvailabilityRule> rules,
                                                     Set<LocalDate> exceptionDates) {
        if (exceptionDates.contains(day)) {
            return List.of();
        }
        List<LocalDateTime> daySlots = new ArrayList<>();
        for (WeeklyAvailabilityRule rule : rules) {
            if (rule.dayOfWeek() != day.getDayOfWeek()) {
                continue;
            }
            LocalTime time = rule.startTime();
            while (!time.plusMinutes(rule.slotDurationMinutes()).isAfter(rule.endTime())) {
                daySlots.add(LocalDateTime.of(day, time));
                time = time.plusMinutes(rule.slotDurationMinutes());
            }
        }
        return daySlots;
    }
}
