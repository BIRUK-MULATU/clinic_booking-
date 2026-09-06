package et.aau.clinic.unit;

import et.aau.clinic.core.AppointmentEvent;
import et.aau.clinic.core.AppointmentStateMachine;
import et.aau.clinic.domain.AppointmentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.stream.Stream;

import static et.aau.clinic.core.AppointmentEvent.ATTEND;
import static et.aau.clinic.core.AppointmentEvent.CANCEL;
import static et.aau.clinic.core.AppointmentEvent.CONFIRM;
import static et.aau.clinic.core.AppointmentEvent.EXPIRE_OFFER;
import static et.aau.clinic.core.AppointmentEvent.MARK_NO_SHOW;
import static et.aau.clinic.core.AppointmentEvent.PROMOTE;
import static et.aau.clinic.core.AppointmentEvent.RESCHEDULE;
import static et.aau.clinic.domain.AppointmentStatus.ATTENDED;
import static et.aau.clinic.domain.AppointmentStatus.CANCELLED;
import static et.aau.clinic.domain.AppointmentStatus.CONFIRMED;
import static et.aau.clinic.domain.AppointmentStatus.NO_SHOW;
import static et.aau.clinic.domain.AppointmentStatus.OFFER_EXPIRED;
import static et.aau.clinic.domain.AppointmentStatus.REQUESTED;
import static et.aau.clinic.domain.AppointmentStatus.WAITLISTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Rule 3 - state transition testing over the 7 states x 7 events grid
 * (49 pairs: 10 valid, 39 invalid). The grid has grown by extension
 * three times: Phase C added WAITLISTED + PROMOTE, Rule I added
 * RESCHEDULE (a state-keeping self-loop from REQUESTED and CONFIRMED),
 * and Rule J added the terminal OFFER_EXPIRED state + EXPIRE_OFFER
 * (REQUESTED to OFFER_EXPIRED). Also covers Rule 3b's late-cancellation
 * fee, a guard condition tested with its own BVA on the 24-hour mark.
 */
class AppointmentStateMachineTest {

    private static final LocalDateTime SLOT_START = LocalDateTime.of(2026, 1, 10, 14, 0);

    // TC-S01 - State transition table: REQUESTED --confirm--> CONFIRMED (valid).
    @Test
    void transition_requestedConfirm_movesToConfirmed() {
        assertThat(AppointmentStateMachine.transition(REQUESTED, CONFIRM)).isEqualTo(CONFIRMED);
    }

    // TC-S02 - State transition table: REQUESTED --cancel--> CANCELLED (valid).
    @Test
    void transition_requestedCancel_movesToCancelled() {
        assertThat(AppointmentStateMachine.transition(REQUESTED, CANCEL)).isEqualTo(CANCELLED);
    }

    // TC-S03 - State transition table: CONFIRMED --attend--> ATTENDED (valid).
    @Test
    void transition_confirmedAttend_movesToAttended() {
        assertThat(AppointmentStateMachine.transition(CONFIRMED, ATTEND)).isEqualTo(ATTENDED);
    }

    // TC-S04 - State transition table: CONFIRMED --cancel--> CANCELLED (valid).
    @Test
    void transition_confirmedCancel_movesToCancelled() {
        assertThat(AppointmentStateMachine.transition(CONFIRMED, CANCEL)).isEqualTo(CANCELLED);
    }

    // TC-S05 - State transition table: CONFIRMED --markNoShow--> NO_SHOW (valid).
    @Test
    void transition_confirmedMarkNoShow_movesToNoShow() {
        assertThat(AppointmentStateMachine.transition(CONFIRMED, MARK_NO_SHOW)).isEqualTo(NO_SHOW);
    }

    // TC-S09 - Hospital-expansion Phase C, state transition table: WAITLISTED --promote-->
    // REQUESTED (valid) - the slot freed up and this appointment moves off the waitlist.
    @Test
    void transition_waitlistedPromote_movesToRequested() {
        assertThat(AppointmentStateMachine.transition(WAITLISTED, PROMOTE)).isEqualTo(REQUESTED);
    }

    // TC-S10 - Hospital-expansion Phase C, state transition table: WAITLISTED --cancel-->
    // CANCELLED (valid) - a patient can leave the waitlist without ever being promoted.
    @Test
    void transition_waitlistedCancel_movesToCancelled() {
        assertThat(AppointmentStateMachine.transition(WAITLISTED, CANCEL)).isEqualTo(CANCELLED);
    }

    // TC-S11 - Rule I, state transition table: REQUESTED --reschedule--> REQUESTED (valid) -
    // a slot move keeps the appointment in its current state.
    @Test
    void transition_requestedReschedule_staysRequested() {
        assertThat(AppointmentStateMachine.transition(REQUESTED, RESCHEDULE)).isEqualTo(REQUESTED);
    }

    // TC-S12 - Rule I, state transition table: CONFIRMED --reschedule--> CONFIRMED (valid).
    @Test
    void transition_confirmedReschedule_staysConfirmed() {
        assertThat(AppointmentStateMachine.transition(CONFIRMED, RESCHEDULE)).isEqualTo(CONFIRMED);
    }

    // TC-S13 - Rule J, state transition table: REQUESTED --expireOffer--> OFFER_EXPIRED (valid) -
    // a promoted waitlist offer the patient did not confirm within 2 hours.
    @Test
    void transition_requestedExpireOffer_movesToOfferExpired() {
        assertThat(AppointmentStateMachine.transition(REQUESTED, EXPIRE_OFFER)).isEqualTo(OFFER_EXPIRED);
    }

    // State transition table: covers all 39 invalid (state, event) pairs - the full 7x7 grid
    // (49 pairs) minus the 10 valid transitions TC-S01-TC-S05, TC-S09-TC-S13 cover above.
    @ParameterizedTest(name = "{index}: {0} + {1} is invalid")
    @MethodSource("invalidStateEventPairs")
    void transition_invalidPair_throwsIllegalStateException(AppointmentStatus state, AppointmentEvent event) {
        assertThatThrownBy(() -> AppointmentStateMachine.transition(state, event))
                .isInstanceOf(IllegalStateException.class);
    }

    /**
     * State transition table: data source for the 39 invalid pairs above.
     * All 49 state/event pairs (7 states x 7 events) minus the 10 valid
     * ones - 5 from CLAUDE.md's original table, 2 from Phase C, 2 from
     * Rule I (RESCHEDULE) and 1 from Rule J (EXPIRE_OFFER) - generated
     * rather than hand-listed so the count (39) is enforced by the grid
     * itself, not by hand-copying.
     */
    static Stream<Arguments> invalidStateEventPairs() {
        record Valid(AppointmentStatus state, AppointmentEvent event) {
        }
        var validPairs = Stream.of(
                new Valid(REQUESTED, CONFIRM),
                new Valid(REQUESTED, CANCEL),
                new Valid(REQUESTED, RESCHEDULE),
                new Valid(REQUESTED, EXPIRE_OFFER),
                new Valid(CONFIRMED, ATTEND),
                new Valid(CONFIRMED, CANCEL),
                new Valid(CONFIRMED, MARK_NO_SHOW),
                new Valid(CONFIRMED, RESCHEDULE),
                new Valid(WAITLISTED, PROMOTE),
                new Valid(WAITLISTED, CANCEL)
        ).toList();

        return EnumSet.allOf(AppointmentStatus.class).stream()
                .flatMap(state -> EnumSet.allOf(AppointmentEvent.class).stream()
                        .filter(event -> validPairs.stream()
                                .noneMatch(v -> v.state() == state && v.event() == event))
                        .map(event -> Arguments.of(state, event)));
    }

    // Meta-check on the state transition table itself, not a technique-derived case: asserts
    // the generated invalid set has exactly 39 members, so a future edit to the grid can't
    // silently drop or duplicate a pair.
    //
    // Hand-derivation of valid vs invalid on the full 7 x 7 = 49 grid, by state row:
    //   REQUESTED    : CONFIRM, CANCEL, RESCHEDULE, EXPIRE_OFFER valid (4); ATTEND,
    //                  MARK_NO_SHOW, PROMOTE invalid (3).
    //   CONFIRMED    : ATTEND, CANCEL, MARK_NO_SHOW, RESCHEDULE valid (4); CONFIRM,
    //                  PROMOTE, EXPIRE_OFFER invalid (3).
    //   WAITLISTED   : PROMOTE, CANCEL valid (2); other 5 invalid.
    //   ATTENDED     : terminal - all 7 invalid.
    //   CANCELLED    : terminal - all 7 invalid.
    //   NO_SHOW      : terminal - all 7 invalid.
    //   OFFER_EXPIRED: terminal - all 7 invalid.
    // Valid = 4 + 4 + 2 = 10. Invalid = 3 + 3 + 5 + 7 + 7 + 7 + 7 = 39. 10 + 39 = 49. OK.
    @Test
    void invalidStateEventPairs_containsExactlyThirtyNinePairs() {
        assertThat(invalidStateEventPairs().count()).isEqualTo(39);
    }

    // TC-S06 - BVA on Rule 3b's 24h guard: just inside the late window (23h59m).
    @Test
    void lateCancellationFee_atTwentyThreeHours59Minutes_chargesHalfFee() {
        LocalDateTime now = SLOT_START.minusHours(23).minusMinutes(59);

        BigDecimal fee = AppointmentStateMachine.lateCancellationFee(now, SLOT_START, new BigDecimal("250"));

        assertThat(fee).isEqualByComparingTo(new BigDecimal("125.0"));
    }

    // TC-S07 - BVA on Rule 3b's 24h guard: the boundary itself (exactly 24h00m, free per "less than").
    @Test
    void lateCancellationFee_atExactlyTwentyFourHours_isFree() {
        LocalDateTime now = SLOT_START.minusHours(24);

        BigDecimal fee = AppointmentStateMachine.lateCancellationFee(now, SLOT_START, new BigDecimal("250"));

        assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // TC-S08 - BVA on Rule 3b's 24h guard: just outside the late window (24h01m).
    @Test
    void lateCancellationFee_atTwentyFourHoursOneMinute_isFree() {
        LocalDateTime now = SLOT_START.minusHours(24).minusMinutes(1);

        BigDecimal fee = AppointmentStateMachine.lateCancellationFee(now, SLOT_START, new BigDecimal("250"));

        assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
