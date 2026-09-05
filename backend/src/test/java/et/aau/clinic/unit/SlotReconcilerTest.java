package et.aau.clinic.unit;

import et.aau.clinic.core.SlotReconciliation;
import et.aau.clinic.core.SlotReconciler;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hospital-expansion Phase B, SlotReconciler - equivalence partitioning
 * over the four categories a slot time can fall into when a schedule is
 * re-expanded: unchanged, new, orphaned-and-free, orphaned-and-booked.
 * TC-R01/TC-R02 also prove the "unchanged" partition's outcome really
 * doesn't depend on booked-ness (a don't-care), the same style
 * BookingPolicyTest uses to prove its own priority ordering.
 */
class SlotReconcilerTest {

    private static final LocalDateTime T1 = LocalDateTime.of(2026, 1, 10, 9, 0);
    private static final LocalDateTime T2 = LocalDateTime.of(2026, 1, 10, 9, 30);
    private static final LocalDateTime T3 = LocalDateTime.of(2026, 1, 10, 10, 0);
    private static final LocalDateTime T4 = LocalDateTime.of(2026, 1, 10, 10, 30);

    // TC-R01 - EP: "unchanged" partition (in old and new), unbooked -> no-op.
    @Test
    void reconcile_unchangedUnbookedSlot_isNeitherCreatedNorRemoved() {
        SlotReconciliation result = SlotReconciler.reconcile(Set.of(T1), Set.of(T1), Set.of());

        assertThat(result.toCreate()).isEmpty();
        assertThat(result.toRemove()).isEmpty();
    }

    // TC-R02 - EP: "unchanged" partition, booked variant -> still a no-op. Proves booked-ness
    // is a don't-care for this partition, since the slot is still on the current schedule.
    @Test
    void reconcile_unchangedBookedSlot_isNeitherCreatedNorRemoved() {
        SlotReconciliation result = SlotReconciler.reconcile(Set.of(T1), Set.of(T1), Set.of(T1));

        assertThat(result.toCreate()).isEmpty();
        assertThat(result.toRemove()).isEmpty();
    }

    // TC-R03 - EP: "new" partition (in new schedule, not yet persisted) -> created.
    @Test
    void reconcile_newCandidateSlot_isCreated() {
        SlotReconciliation result = SlotReconciler.reconcile(Set.of(T1), Set.of(), Set.of());

        assertThat(result.toCreate()).containsExactly(T1);
        assertThat(result.toRemove()).isEmpty();
    }

    // TC-R04 - EP: "orphaned, free" partition (was on the old schedule, not on the new one,
    // nobody booked it) -> removed.
    @Test
    void reconcile_orphanedFreeSlot_isRemoved() {
        SlotReconciliation result = SlotReconciler.reconcile(Set.of(), Set.of(T1), Set.of());

        assertThat(result.toCreate()).isEmpty();
        assertThat(result.toRemove()).containsExactly(T1);
    }

    // TC-R05 - EP: "orphaned, booked" partition - the critical case. A slot that no longer
    // matches the schedule but has an active appointment must NEVER appear in toRemove,
    // asserted directly here rather than only implied by the other cases.
    @Test
    void reconcile_orphanedBookedSlot_isNeverRemoved() {
        SlotReconciliation result = SlotReconciler.reconcile(Set.of(), Set.of(T1), Set.of(T1));

        assertThat(result.toRemove()).doesNotContain(T1);
        assertThat(result.toCreate()).isEmpty();
    }

    // TC-R06 - all four partitions exercised together in one call, proving no cross-partition
    // interference: T1 unchanged, T2 new, T3 orphaned+free, T4 orphaned+booked (protected).
    @Test
    void reconcile_allFourPartitionsAtOnce_eachHandledIndependently() {
        Set<LocalDateTime> candidates = Set.of(T1, T2);
        Set<LocalDateTime> existing = Set.of(T1, T3, T4);
        Set<LocalDateTime> booked = Set.of(T4);

        SlotReconciliation result = SlotReconciler.reconcile(candidates, existing, booked);

        assertThat(result.toCreate()).containsExactly(T2);
        assertThat(result.toRemove()).containsExactly(T3);
        assertThat(result.toRemove()).doesNotContain(T4);
    }
}
