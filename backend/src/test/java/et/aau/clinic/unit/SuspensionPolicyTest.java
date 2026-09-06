package et.aau.clinic.unit;

import et.aau.clinic.core.SuspensionDecision;
import et.aau.clinic.core.SuspensionPolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rule J (hospital-expansion) - the "three strikes" no-show suspension.
 * Two independent boundary value targets:
 *
 *   Count boundary (threshold = 3): 2 recent no-shows is clear, 3 is
 *   suspended, 4 is still suspended.
 *
 *   Window boundary (90 days): a no-show exactly 90 days before "now"
 *   still counts (inclusive edge); one 90 days + 1 has aged out; one
 *   89 days ago is well inside.
 *
 * Equivalence partitions: each no-show timestamp is either inside the
 * 90-day window or aged out; the recent count is either 0-2 (clear) or
 * 3+ (suspended). NOW is fixed so every "days ago" is exact.
 */
class SuspensionPolicyTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 1, 10, 0);

    // TC-H01 - count BVA: no no-shows at all is clear.
    @Test
    void suspension_noNoShows_isClear() {
        SuspensionDecision decision = SuspensionPolicy.evaluate(List.of(), NOW);

        assertClear(decision, 0);
    }

    // TC-H02 - count BVA: 2 recent no-shows is one below the threshold - still clear.
    @Test
    void suspension_twoRecentNoShows_isClear() {
        SuspensionDecision decision = SuspensionPolicy.evaluate(
                List.of(NOW.minusDays(5), NOW.minusDays(30)), NOW);

        assertClear(decision, 2);
    }

    // TC-H03 - count BVA: 3 recent no-shows hits the threshold - suspended.
    @Test
    void suspension_threeRecentNoShows_isSuspended() {
        SuspensionDecision decision = SuspensionPolicy.evaluate(
                List.of(NOW.minusDays(5), NOW.minusDays(30), NOW.minusDays(60)), NOW);

        assertSuspended(decision, 3);
    }

    // TC-H04 - count BVA: 4 recent no-shows is above the threshold - still suspended.
    @Test
    void suspension_fourRecentNoShows_isSuspended() {
        SuspensionDecision decision = SuspensionPolicy.evaluate(
                List.of(NOW.minusDays(1), NOW.minusDays(5), NOW.minusDays(30), NOW.minusDays(60)), NOW);

        assertSuspended(decision, 4);
    }

    // TC-H05 - window BVA: the third no-show is exactly 90 days ago (inclusive edge) - it counts,
    // so the patient is suspended.
    @Test
    void suspension_thirdNoShowExactly90DaysAgo_stillCounts_isSuspended() {
        SuspensionDecision decision = SuspensionPolicy.evaluate(
                List.of(NOW.minusDays(10), NOW.minusDays(40), NOW.minusDays(90)), NOW);

        assertSuspended(decision, 3);
    }

    // TC-H06 - window BVA: the third no-show is 90 days + 1 ago - it has aged out, leaving only
    // 2 in the window, so the patient is clear.
    @Test
    void suspension_thirdNoShow90DaysAndOneAgo_hasAgedOut_isClear() {
        SuspensionDecision decision = SuspensionPolicy.evaluate(
                List.of(NOW.minusDays(10), NOW.minusDays(40), NOW.minusDays(90).minusMinutes(1)), NOW);

        assertClear(decision, 2);
    }

    // TC-H07 - window EP: old no-shows (well outside 90 days) are ignored entirely even when there
    // are many of them.
    @Test
    void suspension_manyOldNoShows_areIgnored_isClear() {
        SuspensionDecision decision = SuspensionPolicy.evaluate(
                List.of(NOW.minusDays(200), NOW.minusDays(300), NOW.minusDays(400), NOW.minusDays(500)), NOW);

        assertClear(decision, 0);
    }

    // TC-H08 - mixed: 2 inside the window + 1 aged out = clear; adding one more inside tips it.
    @Test
    void suspension_twoInsideOneOut_isClear_butThreeInsideIsSuspended() {
        List<LocalDateTime> twoInsideOneOut =
                List.of(NOW.minusDays(3), NOW.minusDays(89), NOW.minusDays(120));
        assertClear(SuspensionPolicy.evaluate(twoInsideOneOut, NOW), 2);

        List<LocalDateTime> threeInside =
                List.of(NOW.minusDays(3), NOW.minusDays(45), NOW.minusDays(89));
        assertSuspended(SuspensionPolicy.evaluate(threeInside, NOW), 3);
    }

    private static void assertClear(SuspensionDecision decision, long expectedCount) {
        assertThat(decision.isSuspended()).isFalse();
        assertThat(decision.getRecentNoShowCount()).isEqualTo(expectedCount);
    }

    private static void assertSuspended(SuspensionDecision decision, long expectedCount) {
        assertThat(decision.isSuspended()).isTrue();
        assertThat(decision.getRecentNoShowCount()).isEqualTo(expectedCount);
    }
}
