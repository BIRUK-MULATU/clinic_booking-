package et.aau.clinic.core;

import et.aau.clinic.domain.RegistrationRejection;

/**
 * The outcome of PatientRegistrationPolicy: either approved, or rejected
 * with the one reason that stopped it. Same result-object shape as
 * BookingDecision and VisitRecordDecision.
 */
public final class RegistrationDecision {

    private final boolean approved;
    private final RegistrationRejection reason;

    private RegistrationDecision(boolean approved, RegistrationRejection reason) {
        this.approved = approved;
        this.reason = reason;
    }

    public static RegistrationDecision approve() {
        return new RegistrationDecision(true, null);
    }

    public static RegistrationDecision reject(RegistrationRejection reason) {
        return new RegistrationDecision(false, reason);
    }

    public boolean isApproved() {
        return approved;
    }

    public RegistrationRejection getReason() {
        return reason;
    }
}
