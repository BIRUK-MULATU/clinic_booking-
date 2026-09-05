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
import static et.aau.clinic.core.AppointmentEvent.MARK_NO_SHOW;
import static et.aau.clinic.core.AppointmentEvent.PROMOTE;
import static et.aau.clinic.domain.AppointmentStatus.ATTENDED;
import static et.aau.clinic.domain.AppointmentStatus.CANCELLED;
import static et.aau.clinic.domain.AppointmentStatus.CONFIRMED;
import static et.aau.clinic.domain.AppointmentStatus.NO_SHOW;
import static et.aau.clinic.domain.AppointmentStatus.REQUESTED;
import static et.aau.clinic.domain.AppointmentStatus.WAITLISTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Rule 3 - state transition testing over the 6 states x 5 events grid
 * (30 pairs: 7 valid, 23 invalid) after hospital-expansion Phase C added
 * WAITLISTED and PROMOTE, plus Rule 3b's late-cancellation fee, a guard
 * condition tested with its own BVA on the 24-hour mark.
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

    // State transition table: covers all 23 invalid (state, event) pairs - the full 6x5 grid
    // (30 pairs) minus the 7 valid transitions TC-S01-TC-S05, TC-S09, TC-S10 cover above.
    @ParameterizedTest(name = "{index}: {0} + {1} is invalid")
    @MethodSource("invalidStateEventPairs")
    void transition_invalidPair_throwsIllegalStateException(AppointmentStatus state, AppointmentEvent event) {
        assertThatThrownBy(() -> AppointmentStateMachine.transition(state, event))
                .isInstanceOf(IllegalStateException.class);
    }

    /**
     * State transition table: data source for the 23 invalid pairs above.
     * All 30 state/event pairs (6 states x 5 events, after hospital-
     * expansion Phase C added WAITLISTED and PROMOTE) minus the 7 valid
     * ones - 5 from CLAUDE.md's original table plus the 2 Phase C added -
     * generated rather than hand-listed so the count (23) is enforced by
     * the grid itself, not by hand-copying.
     */
    static Stream<Arguments> invalidStateEventPairs() {
        record Valid(AppointmentStatus state, AppointmentEvent event) {
        }
        var validPairs = Stream.of(
                new Valid(REQUESTED, CONFIRM),
                new Valid(REQUESTED, CANCEL),
                new Valid(CONFIRMED, ATTEND),
                new Valid(CONFIRMED, CANCEL),
                new Valid(CONFIRMED, MARK_NO_SHOW),
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
    // the generated invalid set has exactly 23 members, so a future edit to the grid can't
    // silently drop or duplicate a pair.
    //
    // Hand-derivation, 15 -> 23 (hospital-expansion Phase C added WAITLISTED and PROMOTE):
    // the grid doesn't grow by addition, it grows by area. One new state adds a full new
    // ROW (5 cells: WAITLISTED x each of the 5 events); one new event adds a full new
    // COLUMN (6 cells: PROMOTE x each of the 6 states); they overlap at exactly one cell
    // (WAITLISTED x PROMOTE). New cells = 5 + 6 - 1 = 10, decomposed as:
    //   - old 5 states x new PROMOTE column (5 cells): all invalid - PROMOTE only ever
    //     makes sense from WAITLISTED.
    //   - new WAITLISTED row x old 4 events (4 cells): CANCEL valid, CONFIRM/ATTEND/
    //     MARK_NO_SHOW invalid (3 invalid, 1 valid).
    //   - the 1 overlap cell, WAITLISTED x PROMOTE: valid.
    // Of the 10 new cells: 2 valid (WAITLISTED+PROMOTE, WAITLISTED+CANCEL), 8 invalid
    // (5 + 3 above). The original 15 invalid pairs are untouched (the original 5x4 block
    // is unchanged), so invalid = 15 + 8 = 23. Sanity check: valid = 5 + 2 = 7,
    // 7 + 23 = 30 = 6 x 5. Both totals agree.
    @Test
    void invalidStateEventPairs_containsExactlyTwentyThreePairs() {
        assertThat(invalidStateEventPairs().count()).isEqualTo(23);
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
