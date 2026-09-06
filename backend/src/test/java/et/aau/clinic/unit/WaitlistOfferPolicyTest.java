package et.aau.clinic.unit;

import et.aau.clinic.core.WaitlistOfferPolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rule J (hospital-expansion) - a promoted waitlist offer expires 2
 * hours after it was made. A single guard condition, tested with
 * boundary value analysis on the 2-hour window: 1h59m after the offer
 * is still live, exactly 2h00m has expired (the window is inclusive at
 * the boundary, like Rule F's reminder), 2h01m has expired.
 */
class WaitlistOfferPolicyTest {

    private static final LocalDateTime OFFERED_AT = LocalDateTime.of(2026, 4, 1, 12, 0);

    // TC-J01 - BVA: 1h59m after the offer - still within the acceptance window.
    @Test
    void offer_oneHour59MinutesOld_isNotExpired() {
        LocalDateTime now = OFFERED_AT.plusHours(1).plusMinutes(59);

        assertThat(WaitlistOfferPolicy.isExpired(OFFERED_AT, now)).isFalse();
    }

    // TC-J02 - BVA: exactly 2h00m after the offer - the boundary itself has expired.
    @Test
    void offer_exactly2HoursOld_isExpired() {
        LocalDateTime now = OFFERED_AT.plusHours(2);

        assertThat(WaitlistOfferPolicy.isExpired(OFFERED_AT, now)).isTrue();
    }

    // TC-J03 - BVA: 2h01m after the offer - just past the boundary.
    @Test
    void offer_2HoursAndOneMinuteOld_isExpired() {
        LocalDateTime now = OFFERED_AT.plusHours(2).plusMinutes(1);

        assertThat(WaitlistOfferPolicy.isExpired(OFFERED_AT, now)).isTrue();
    }

    // TC-J04 - sanity: an offer made just now is nowhere near expired.
    @Test
    void offer_justMade_isNotExpired() {
        assertThat(WaitlistOfferPolicy.isExpired(OFFERED_AT, OFFERED_AT.plusMinutes(1))).isFalse();
    }
}
