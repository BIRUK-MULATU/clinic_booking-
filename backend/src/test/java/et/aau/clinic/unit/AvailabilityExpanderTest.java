package et.aau.clinic.unit;

import et.aau.clinic.core.AvailabilityExpander;
import et.aau.clinic.core.WeeklyAvailabilityRule;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hospital-expansion Phase B, AvailabilityExpander - decision table over
 * C1 (day-of-week matches a rule) and C2 (date is an exception), C2
 * outranking C1 exactly like C1 outranking C2/C3 in BookingPolicy, plus
 * BVA on two independent boundaries: the slot-duration/window-end fit,
 * and the 14-day generation horizon itself.
 *
 * FROM is a fixed Monday (2026-01-05) throughout, so day-of-week
 * arithmetic in each test is easy to verify by hand.
 */
class AvailabilityExpanderTest {

    private static final LocalDate FROM = LocalDate.of(2026, 1, 5); // a Monday

    // TC-E01 - Decision table: C1=T (day-of-week matches), C2=F (no exception) -> slot generated.
    // MONDAY recurs twice within the 14-day horizon (FROM and FROM+7) - that's correct weekly
    // recurrence, not a bug, so this only asserts FROM's own slot is among the results.
    @Test
    void expand_dayMatchesRuleAndNotExcepted_generatesSlots() {
        WeeklyAvailabilityRule rule = new WeeklyAvailabilityRule(DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(9, 30), 30);

        List<LocalDateTime> result = AvailabilityExpander.expand(List.of(rule), Set.of(), FROM);

        assertThat(result).contains(LocalDateTime.of(FROM, LocalTime.of(9, 0)));
    }

    // TC-E02 - Decision table: C1=T, C2=T (excepted) -> no slots on that specific day. Proves
    // the exception (C2) overrides an otherwise-matching rule (C1) for FROM itself, the same
    // priority shape as C1 outranking C2/C3 in BookingPolicy - the rule's other occurrence
    // (FROM+7, not excepted) is unaffected, which is correct: an exception is a single date.
    @Test
    void expand_dayMatchesRuleButIsExcepted_generatesNoSlots() {
        WeeklyAvailabilityRule rule = new WeeklyAvailabilityRule(DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(9, 30), 30);

        List<LocalDateTime> result = AvailabilityExpander.expand(List.of(rule), Set.of(FROM), FROM);

        assertThat(result).noneMatch(dateTime -> dateTime.toLocalDate().equals(FROM));
    }

    // TC-E03 - Decision table: C1=F (no rule matches this day-of-week), C2=F -> no slots.
    @Test
    void expand_dayMatchesNoRule_generatesNoSlots() {
        WeeklyAvailabilityRule tuesdayRule = new WeeklyAvailabilityRule(DayOfWeek.TUESDAY,
                LocalTime.of(9, 0), LocalTime.of(9, 30), 30);

        List<LocalDateTime> result = AvailabilityExpander.expand(List.of(tuesdayRule), Set.of(), FROM);

        assertThat(result).noneMatch(dateTime -> dateTime.toLocalDate().equals(FROM));
    }

    // TC-E04 - BVA on the slot-fit boundary: the window divides evenly by the slot duration,
    // so the last slot's end lands exactly on endTime (inclusive boundary). Filtered to FROM's
    // date since MONDAY's other occurrence (FROM+7) would otherwise duplicate the same pattern.
    @Test
    void expand_windowDividesEvenlyBySlotDuration_lastSlotEndsExactlyAtEndTime() {
        WeeklyAvailabilityRule rule = new WeeklyAvailabilityRule(DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(10, 0), 30);

        List<LocalDateTime> result = AvailabilityExpander.expand(List.of(rule), Set.of(), FROM);
        List<LocalDateTime> onFrom = result.stream().filter(dt -> dt.toLocalDate().equals(FROM)).toList();

        assertThat(onFrom).containsExactly(
                LocalDateTime.of(FROM, LocalTime.of(9, 0)),
                LocalDateTime.of(FROM, LocalTime.of(9, 30)));
    }

    // TC-E05 - BVA on the slot-fit boundary: leftover time shorter than one slot duration is
    // not enough for another slot, so it's dropped rather than overflowing past endTime.
    @Test
    void expand_leftoverTimeShorterThanOneSlot_noOverflowingSlotGenerated() {
        WeeklyAvailabilityRule rule = new WeeklyAvailabilityRule(DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(9, 45), 30);

        List<LocalDateTime> result = AvailabilityExpander.expand(List.of(rule), Set.of(), FROM);
        List<LocalDateTime> onFrom = result.stream().filter(dt -> dt.toLocalDate().equals(FROM)).toList();

        assertThat(onFrom).containsExactly(LocalDateTime.of(FROM, LocalTime.of(9, 0)));
    }

    // TC-E06 - BVA on the horizon boundary: day offset 13 (the last included day of the
    // 14-day window) still produces a slot.
    @Test
    void expand_atLastIncludedHorizonDay_generatesSlot() {
        LocalDate lastIncludedDay = FROM.plusDays(AvailabilityExpander.HORIZON_DAYS - 1);
        WeeklyAvailabilityRule rule = new WeeklyAvailabilityRule(lastIncludedDay.getDayOfWeek(),
                LocalTime.of(9, 0), LocalTime.of(9, 30), 30);

        List<LocalDateTime> result = AvailabilityExpander.expand(List.of(rule), Set.of(), FROM);

        assertThat(result).contains(LocalDateTime.of(lastIncludedDay, LocalTime.of(9, 0)));
    }

    // TC-E07 - BVA on the horizon boundary: day offset 14 (the first excluded day) generates
    // no slot even though its day-of-week matches a rule - proves the cutoff is the horizon
    // itself, not a day-of-week mismatch.
    @Test
    void expand_atFirstExcludedHorizonDay_generatesNoSlotDespiteMatchingRule() {
        LocalDate firstExcludedDay = FROM.plusDays(AvailabilityExpander.HORIZON_DAYS);
        WeeklyAvailabilityRule rule = new WeeklyAvailabilityRule(firstExcludedDay.getDayOfWeek(),
                LocalTime.of(9, 0), LocalTime.of(9, 30), 30);

        List<LocalDateTime> result = AvailabilityExpander.expand(List.of(rule), Set.of(), FROM);

        assertThat(result).noneMatch(dateTime -> dateTime.toLocalDate().equals(firstExcludedDay));
    }
}
