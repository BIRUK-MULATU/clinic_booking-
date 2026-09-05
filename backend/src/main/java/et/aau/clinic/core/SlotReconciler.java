package et.aau.clinic.core;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Diffs freshly-expanded candidate slot times against what's already
 * persisted, without ever touching a slot someone has booked. Every
 * slot time falls into exactly one of four partitions:
 *
 *   in old | in new | booked | action
 *   -------+--------+--------+------------------------------
 *     yes  |  yes   |  n/a   | unchanged (no-op)
 *     no   |  yes   |   -    | new -> create
 *     yes  |  no    |  no    | orphaned, free -> remove
 *     yes  |  no    |  yes   | orphaned, booked -> PROTECTED, never removed
 *
 * The fourth row is the whole point: a booked slot survives a schedule
 * change because this function is defined to never delete a slot with
 * an active appointment, regardless of whether it still matches the
 * current schedule.
 */
public final class SlotReconciler {

    private SlotReconciler() {
    }

    public static SlotReconciliation reconcile(Set<LocalDateTime> candidates, Set<LocalDateTime> existing,
                                                 Set<LocalDateTime> booked) {
        Set<LocalDateTime> toCreate = new HashSet<>(candidates);
        toCreate.removeAll(existing);

        Set<LocalDateTime> toRemove = new HashSet<>(existing);
        toRemove.removeAll(candidates);
        toRemove.removeAll(booked);

        return new SlotReconciliation(toCreate, toRemove);
    }
}
