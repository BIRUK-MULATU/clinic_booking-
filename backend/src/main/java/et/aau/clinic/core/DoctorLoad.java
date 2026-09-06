package et.aau.clinic.core;

import et.aau.clinic.domain.DoctorLoadStatus;

/**
 * Rule G (hospital-expansion: doctor capacity). A pure function that
 * turns a doctor's daily patient limit and the number of patients
 * already scheduled with them on a given day into a load status and a
 * remaining count.
 *
 *   scheduled >= dailyLimit  -> FULL        (at or over capacity)
 *   exactly one place left    -> NEARLY_FULL
 *   otherwise                 -> AVAILABLE
 *
 * remaining is clamped at zero, so an over-booked day (scheduled beyond
 * the limit, which reception is allowed to do) still reports 0 left
 * rather than a negative number.
 *
 * Boundary value targets: with a limit of L, the values L-2, L-1, L and
 * L+1 for "scheduled" - the edges of the AVAILABLE / NEARLY_FULL / FULL
 * partitions.
 */
public record DoctorLoad(int dailyLimit, int scheduled, int remaining, DoctorLoadStatus status) {

    public static DoctorLoad evaluate(int dailyLimit, int scheduled) {
        int remaining = Math.max(0, dailyLimit - scheduled);
        DoctorLoadStatus status;
        if (scheduled >= dailyLimit) {
            status = DoctorLoadStatus.FULL;
        } else if (remaining == 1) {
            status = DoctorLoadStatus.NEARLY_FULL;
        } else {
            status = DoctorLoadStatus.AVAILABLE;
        }
        return new DoctorLoad(dailyLimit, scheduled, remaining, status);
    }

    public boolean isOverLimit() {
        return scheduled > dailyLimit;
    }
}
