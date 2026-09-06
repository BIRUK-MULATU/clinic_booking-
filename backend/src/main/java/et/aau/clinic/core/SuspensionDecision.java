package et.aau.clinic.core;

/**
 * The outcome of SuspensionPolicy: whether the patient is currently
 * barred from booking themselves in, and how many recent no-shows drove
 * that. A result object rather than a bare boolean, matching
 * BookingDecision - the count is useful to show the patient ("2 of 3")
 * and to assert on in tests.
 */
public final class SuspensionDecision {

    private final boolean suspended;
    private final long recentNoShowCount;

    private SuspensionDecision(boolean suspended, long recentNoShowCount) {
        this.suspended = suspended;
        this.recentNoShowCount = recentNoShowCount;
    }

    public static SuspensionDecision suspended(long recentNoShowCount) {
        return new SuspensionDecision(true, recentNoShowCount);
    }

    public static SuspensionDecision clear(long recentNoShowCount) {
        return new SuspensionDecision(false, recentNoShowCount);
    }

    public boolean isSuspended() {
        return suspended;
    }

    public long getRecentNoShowCount() {
        return recentNoShowCount;
    }
}
