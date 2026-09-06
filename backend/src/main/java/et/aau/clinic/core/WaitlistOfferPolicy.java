package et.aau.clinic.core;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Rule L (hospital-expansion): a waitlist offer expires if the patient
 * does not confirm within 2 hours of being promoted. After that the
 * slot is offered to the next person on the waitlist instead.
 *
 * A single guard condition with one time threshold, tested with
 * boundary value analysis on the 2-hour window: 1h59m after the offer
 * is still live, exactly 2h00m has expired ("2 hours or more" - the
 * same inclusive sense as Rule H's reminder window, and the opposite of
 * Rule 3b's "less than 24 hours"), 2h01m has expired.
 *
 * Pure: the caller passes the promotion time and "now" from the
 * injected Clock.
 */
public final class WaitlistOfferPolicy {

    static final Duration ACCEPTANCE_WINDOW = Duration.ofHours(2);

    private WaitlistOfferPolicy() {
    }

    public static boolean isExpired(LocalDateTime offeredAt, LocalDateTime now) {
        return Duration.between(offeredAt, now).compareTo(ACCEPTANCE_WINDOW) >= 0;
    }
}
