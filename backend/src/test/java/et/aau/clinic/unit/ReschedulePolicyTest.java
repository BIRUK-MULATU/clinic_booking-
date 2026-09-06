package et.aau.clinic.unit;

import et.aau.clinic.core.RescheduleDecision;
import et.aau.clinic.core.ReschedulePolicy;
import et.aau.clinic.domain.AppointmentStatus;
import et.aau.clinic.domain.RescheduleRejection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;

/**
 * Rule I (hospital-expansion) - moving an appointment to a new slot,
 * evaluated as a decision table over C1 (state is REQUESTED or
 * CONFIRMED), C2 (new slot free), C3 (>= 2h notice on the new slot), in
 * that priority order, plus boundary value analysis on the 2-hour
 * notice threshold - reused from Rule 2's C3, so the boundaries are the
 * same: 1h59m rejects, exactly 2h00m is allowed.
 *
 * The collapsed table has 4 outcome rows. The two rules that sit above a
 * lower-priority condition (R1 above C2/C3, R2 above C3) get an extra
 * test with the don't-care condition(s) flipped, to prove the reported
 * reason does not depend on them.
 */
class ReschedulePolicyTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 9, 0);

    // TC-I01 - Decision table Rule R1 (C1=F): a done appointment cannot be rescheduled.
    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"REQUESTED", "CONFIRMED"}, mode = EXCLUDE)
    void reschedule_appointmentInNonReschedulableState_rejectsWithNotReschedulable(AppointmentStatus status) {
        RescheduleDecision decision = ReschedulePolicy.evaluate(status, true, NOW, NOW.plusDays(1));

        assertRejected(decision, RescheduleRejection.NOT_RESCHEDULABLE);
    }

    // TC-I02 - Decision table Rule R1 (C1=F) with C2/C3 don't-care shown invalid: still R1.
    @Test
    void reschedule_cancelledAppointment_toATakenSlotWithNoNotice_stillRejectsWithNotReschedulable() {
        RescheduleDecision decision =
                ReschedulePolicy.evaluate(AppointmentStatus.CANCELLED, false, NOW, NOW.plusMinutes(10));

        assertRejected(decision, RescheduleRejection.NOT_RESCHEDULABLE);
    }

    // TC-I03 - Decision table Rule R2 (C1=T, C2=F): the target slot is taken.
    @Test
    void reschedule_requestedAppointment_toATakenSlot_rejectsWithSlotUnavailable() {
        RescheduleDecision decision =
                ReschedulePolicy.evaluate(AppointmentStatus.REQUESTED, false, NOW, NOW.plusDays(1));

        assertRejected(decision, RescheduleRejection.SLOT_UNAVAILABLE);
    }

    // TC-I04 - Decision table Rule R2 (C1=T, C2=F) with C3 don't-care shown invalid: still R2.
    @Test
    void reschedule_confirmedAppointment_toATakenSlotWithNoNotice_stillRejectsWithSlotUnavailable() {
        RescheduleDecision decision =
                ReschedulePolicy.evaluate(AppointmentStatus.CONFIRMED, false, NOW, NOW.plusMinutes(30));

        assertRejected(decision, RescheduleRejection.SLOT_UNAVAILABLE);
    }

    // TC-I05 - Decision table Rule R3 (C1=T, C2=T, C3=F) / BVA: new slot 1h59m out - just short.
    @Test
    void reschedule_toAFreeSlotOneHour59MinutesAway_rejectsWithInsufficientNotice() {
        RescheduleDecision decision = ReschedulePolicy.evaluate(
                AppointmentStatus.REQUESTED, true, NOW, NOW.plusHours(1).plusMinutes(59));

        assertRejected(decision, RescheduleRejection.INSUFFICIENT_NOTICE);
    }

    // TC-I06 - BVA: new slot exactly 2h00m out - the boundary is allowed (">= 2 hours").
    @Test
    void reschedule_toAFreeSlotExactly2HoursAway_isApproved() {
        RescheduleDecision decision = ReschedulePolicy.evaluate(
                AppointmentStatus.REQUESTED, true, NOW, NOW.plusHours(2));

        assertThat(decision.isApproved()).isTrue();
        assertThat(decision.getReason()).isNull();
    }

    // TC-I07 - Decision table Rule R4 (all true): a confirmed appointment, free slot, plenty of notice.
    @Test
    void reschedule_confirmedAppointment_toAFreeSlotWellInAdvance_isApproved() {
        RescheduleDecision decision = ReschedulePolicy.evaluate(
                AppointmentStatus.CONFIRMED, true, NOW, NOW.plusDays(3));

        assertThat(decision.isApproved()).isTrue();
    }

    private static void assertRejected(RescheduleDecision decision, RescheduleRejection expected) {
        assertThat(decision.isApproved()).isFalse();
        assertThat(decision.getReason()).isEqualTo(expected);
    }
}
