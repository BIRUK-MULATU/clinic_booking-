package et.aau.clinic.unit;

import et.aau.clinic.core.BookingDecision;
import et.aau.clinic.core.BookingPolicy;
import et.aau.clinic.domain.RejectionReason;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rule 2 - decision table over C1 (slot free), C2 (no outstanding
 * balance), C3 (>= 2 hours notice), evaluated in that priority order.
 *
 * The collapsed table has 4 rules, each with "don't care" columns for
 * the conditions a higher-priority rule already decided. For every
 * rule with a don't-care column, there are two tests: one with the
 * don't-care condition true and one with it false, to prove the
 * outcome really doesn't depend on it (that priority ordering holds).
 * Plus BVA on the 2-hour notice boundary itself.
 */
class BookingPolicyTest {

    private static final LocalDateTime SLOT_START = LocalDateTime.of(2026, 1, 10, 14, 0);

    @Test
    void decision_slotUnavailable_withGoodBalanceAndNotice_rejectsWithSlotUnavailable() {
        BookingDecision decision = BookingPolicy.evaluate(
                false, true, SLOT_START.minusHours(3), SLOT_START);

        assertRejected(decision, RejectionReason.SLOT_UNAVAILABLE);
    }

    @Test
    void decision_slotUnavailable_withBadBalanceAndInsufficientNotice_stillRejectsWithSlotUnavailable() {
        // Proves C1 outranks C2 and C3: both of those are false too, but
        // SLOT_UNAVAILABLE must still be the reported reason.
        BookingDecision decision = BookingPolicy.evaluate(
                false, false, SLOT_START.minusMinutes(30), SLOT_START);

        assertRejected(decision, RejectionReason.SLOT_UNAVAILABLE);
    }

    @Test
    void decision_outstandingBalance_withSufficientNotice_rejectsWithOutstandingBalance() {
        BookingDecision decision = BookingPolicy.evaluate(
                true, false, SLOT_START.minusHours(3), SLOT_START);

        assertRejected(decision, RejectionReason.OUTSTANDING_BALANCE);
    }

    @Test
    void decision_outstandingBalance_withInsufficientNotice_stillRejectsWithOutstandingBalance() {
        // Proves C2 outranks C3.
        BookingDecision decision = BookingPolicy.evaluate(
                true, false, SLOT_START.minusMinutes(30), SLOT_START);

        assertRejected(decision, RejectionReason.OUTSTANDING_BALANCE);
    }

    @Test
    void decision_insufficientNotice_rejectsWithInsufficientNotice() {
        BookingDecision decision = BookingPolicy.evaluate(
                true, true, SLOT_START.minusMinutes(30), SLOT_START);

        assertRejected(decision, RejectionReason.INSUFFICIENT_NOTICE);
    }

    @Test
    void decision_allConditionsTrue_approves() {
        BookingDecision decision = BookingPolicy.evaluate(
                true, true, SLOT_START.minusHours(3), SLOT_START);

        assertThat(decision.isApproved()).isTrue();
        assertThat(decision.getReason()).isNull();
    }

    @Test
    void notice_exactlyTwoHoursBeforeSlot_isSufficient_approves() {
        BookingDecision decision = BookingPolicy.evaluate(
                true, true, SLOT_START.minusHours(2), SLOT_START);

        assertThat(decision.isApproved()).isTrue();
    }

    @Test
    void notice_oneMinuteLessThanTwoHours_isInsufficient_rejects() {
        BookingDecision decision = BookingPolicy.evaluate(
                true, true, SLOT_START.minusHours(2).plusMinutes(1), SLOT_START);

        assertRejected(decision, RejectionReason.INSUFFICIENT_NOTICE);
    }

    @Test
    void notice_wellOverTwoHours_isSufficient_approves() {
        BookingDecision decision = BookingPolicy.evaluate(
                true, true, SLOT_START.minusHours(10), SLOT_START);

        assertThat(decision.isApproved()).isTrue();
    }

    private void assertRejected(BookingDecision decision, RejectionReason expectedReason) {
        assertThat(decision.isApproved()).isFalse();
        assertThat(decision.getReason()).isEqualTo(expectedReason);
    }
}
