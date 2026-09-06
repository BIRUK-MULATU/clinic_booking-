package et.aau.clinic.core;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Rule H (hospital-expansion): the "three strikes" no-show suspension.
 * A patient who fails to show up for 3 or more appointments within a
 * trailing 90-day window may not book themselves in until enough of
 * those no-shows age out of the window. Reception can still book them
 * in person - this rule only gates self-service booking, the same way
 * Rule 2's C2/C3 do.
 *
 * Pure: the caller passes the timestamps of the patient's past
 * no-shows (a repository lookup) and "now" from the injected Clock, so
 * there is no database or Clock dependency here.
 *
 * Two independent boundary value targets:
 *   - the count: 2 no-shows is allowed, 3 is suspended (threshold = 3).
 *   - the window: a no-show exactly 90 days before now still counts
 *     (the window edge is inclusive); 90 days + 1 has aged out.
 * Equivalence partitions on each no-show timestamp: inside the 90-day
 * window vs aged out; and on the recent count: 0-2 (clear) vs 3+
 * (suspended).
 */
public final class SuspensionPolicy {

    static final int SUSPENSION_THRESHOLD = 3;
    static final int WINDOW_DAYS = 90;

    private SuspensionPolicy() {
    }

    public static SuspensionDecision evaluate(List<LocalDateTime> noShowTimes, LocalDateTime now) {
        LocalDateTime windowStart = now.minusDays(WINDOW_DAYS);
        long recent = noShowTimes.stream()
                .filter(when -> !when.isBefore(windowStart))
                .count();
        return recent >= SUSPENSION_THRESHOLD
                ? SuspensionDecision.suspended(recent)
                : SuspensionDecision.clear(recent);
    }
}
