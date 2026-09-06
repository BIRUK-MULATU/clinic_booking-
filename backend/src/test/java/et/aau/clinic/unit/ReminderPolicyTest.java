package et.aau.clinic.unit;

import et.aau.clinic.core.ReminderDecision;
import et.aau.clinic.core.ReminderPolicy;
import et.aau.clinic.domain.AppointmentStatus;
import et.aau.clinic.domain.ReminderSkipReason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;

/**
 * Rule F (hospital-expansion) - decision table over C1 (status
 * CONFIRMED), C2 (not already reminded), C3 (slot not started), C4
 * (slot within 24h), evaluated in that priority order, plus boundary
 * value analysis on the 24-hour reminder window.
 *
 * The collapsed table has 5 outcome rows. For the two rules that sit
 * above a lower-priority condition there is an extra test with the
 * don't-care condition(s) flipped, to prove the reported reason really
 * does not depend on them - i.e. that the priority ordering holds. BVA
 * then pins the values around the 24h boundary: 24h01m (skip), exactly
 * 24h00m (send - the window is inclusive here, unlike Rule 3b), 23h59m
 * (send), and 1 minute (send). C3 gets its own boundary at zero (slot
 * starting exactly now).
 */
class ReminderPolicyTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 10, 10, 0);

    // TC-F01 - Decision table Rule R1 (C1=F): a REQUESTED appointment is never reminded.
    @Test
    void reminder_appointmentStillRequested_skipsWithNotConfirmed() {
        ReminderDecision decision =
                ReminderPolicy.decide(AppointmentStatus.REQUESTED, NOW, NOW.plusHours(5), false);

        assertSkipped(decision, ReminderSkipReason.NOT_CONFIRMED);
    }

    // TC-F02 - Decision table Rule R1 (C1=F) with C2/C3/C4 don't-care shown invalid.
    @Test
    void reminder_cancelledAppointmentAlreadyRemindedAndSlotPast_stillSkipsWithNotConfirmed() {
        // Proves C1 outranks the rest: reminded already AND slot in the past,
        // but NOT_CONFIRMED must still be the reported reason.
        ReminderDecision decision =
                ReminderPolicy.decide(AppointmentStatus.CANCELLED, NOW, NOW.minusHours(1), true);

        assertSkipped(decision, ReminderSkipReason.NOT_CONFIRMED);
    }

    // TC-F03 - every non-CONFIRMED status is treated the same way by C1.
    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = "CONFIRMED", mode = EXCLUDE)
    void reminder_anyNonConfirmedStatus_skipsWithNotConfirmed(AppointmentStatus status) {
        ReminderDecision decision = ReminderPolicy.decide(status, NOW, NOW.plusHours(5), false);

        assertSkipped(decision, ReminderSkipReason.NOT_CONFIRMED);
    }

    // TC-F04 - Decision table Rule R2 (C1=T, C2=F): a reminder was already sent.
    @Test
    void reminder_alreadySentAndSlotWithinWindow_skipsWithAlreadyReminded() {
        ReminderDecision decision =
                ReminderPolicy.decide(AppointmentStatus.CONFIRMED, NOW, NOW.plusHours(5), true);

        assertSkipped(decision, ReminderSkipReason.ALREADY_REMINDED);
    }

    // TC-F05 - Decision table Rule R2 (C1=T, C2=F) with C3 don't-care shown invalid.
    @Test
    void reminder_alreadySentAndSlotInThePast_stillSkipsWithAlreadyReminded() {
        // Proves C2 outranks C3.
        ReminderDecision decision =
                ReminderPolicy.decide(AppointmentStatus.CONFIRMED, NOW, NOW.minusHours(2), true);

        assertSkipped(decision, ReminderSkipReason.ALREADY_REMINDED);
    }

    // TC-F06 - Decision table Rule R3 (C1=T, C2=T, C3=F): the slot has already started.
    @Test
    void reminder_slotStartedOneMinuteAgo_skipsWithSlotAlreadyStarted() {
        ReminderDecision decision =
                ReminderPolicy.decide(AppointmentStatus.CONFIRMED, NOW, NOW.minusMinutes(1), false);

        assertSkipped(decision, ReminderSkipReason.SLOT_ALREADY_STARTED);
    }

    // TC-F07 - BVA on C3: the slot starts at exactly `now` (zero duration counts as started).
    @Test
    void reminder_slotStartsExactlyNow_skipsWithSlotAlreadyStarted() {
        ReminderDecision decision =
                ReminderPolicy.decide(AppointmentStatus.CONFIRMED, NOW, NOW, false);

        assertSkipped(decision, ReminderSkipReason.SLOT_ALREADY_STARTED);
    }

    // TC-F08 - Decision table Rule R4 (C1..C3=T, C4=F): slot outside the 24h window.
    // BVA: 24h01m away - just too early.
    @Test
    void reminder_slot24h01mAway_skipsWithNotYetDue() {
        ReminderDecision decision = ReminderPolicy.decide(
                AppointmentStatus.CONFIRMED, NOW, NOW.plusHours(24).plusMinutes(1), false);

        assertSkipped(decision, ReminderSkipReason.NOT_YET_DUE);
    }

    // TC-F09 - BVA on the 24h window: the boundary itself. Exactly 24h00m away IS reminded
    // (inclusive - deliberately the opposite of Rule 3b's "less than 24 hours").
    @Test
    void reminder_slotExactly24hAway_sends() {
        ReminderDecision decision = ReminderPolicy.decide(
                AppointmentStatus.CONFIRMED, NOW, NOW.plusHours(24), false);

        assertSends(decision);
    }

    // TC-F10 - BVA on the 24h window: just inside (23h59m away).
    @Test
    void reminder_slot23h59mAway_sends() {
        ReminderDecision decision = ReminderPolicy.decide(
                AppointmentStatus.CONFIRMED, NOW, NOW.plusHours(23).plusMinutes(59), false);

        assertSends(decision);
    }

    // TC-F11 - BVA near the other end of the window: 1 minute before the slot.
    @Test
    void reminder_slotOneMinuteAway_sends() {
        ReminderDecision decision = ReminderPolicy.decide(
                AppointmentStatus.CONFIRMED, NOW, NOW.plusMinutes(1), false);

        assertSends(decision);
    }

    // TC-F12 - sanity: a slot several days out is not yet due.
    @Test
    void reminder_slotThreeDaysAway_skipsWithNotYetDue() {
        ReminderDecision decision = ReminderPolicy.decide(
                AppointmentStatus.CONFIRMED, NOW, NOW.plusDays(3), false);

        assertSkipped(decision, ReminderSkipReason.NOT_YET_DUE);
    }

    private static void assertSends(ReminderDecision decision) {
        assertThat(decision.isDue()).isTrue();
        assertThat(decision.getReason()).isNull();
    }

    private static void assertSkipped(ReminderDecision decision, ReminderSkipReason expected) {
        assertThat(decision.isDue()).isFalse();
        assertThat(decision.getReason()).isEqualTo(expected);
    }
}
