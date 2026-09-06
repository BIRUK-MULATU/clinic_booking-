package et.aau.clinic.core;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Result of SlotReconciler.reconcile: which slot times to create, and
 * which to remove. A value object rather than a boolean, matching
 * BookingDecision's style - the caller needs the actual sets, not just
 * a yes/no.
 */
public record SlotReconciliation(Set<LocalDateTime> toCreate, Set<LocalDateTime> toRemove) {
}
