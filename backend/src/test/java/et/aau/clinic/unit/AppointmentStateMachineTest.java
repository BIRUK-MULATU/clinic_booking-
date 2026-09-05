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
import static et.aau.clinic.domain.AppointmentStatus.ATTENDED;
import static et.aau.clinic.domain.AppointmentStatus.CANCELLED;
import static et.aau.clinic.domain.AppointmentStatus.CONFIRMED;
import static et.aau.clinic.domain.AppointmentStatus.NO_SHOW;
import static et.aau.clinic.domain.AppointmentStatus.REQUESTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Rule 3 - state transition testing over the 5 states x 4 events grid
 * (20 pairs: 5 valid, 15 invalid), plus Rule 3b's late-cancellation
 * fee, a guard condition tested with its own BVA on the 24-hour mark.
 */
class AppointmentStateMachineTest {

    private static final LocalDateTime SLOT_START = LocalDateTime.of(2026, 1, 10, 14, 0);

    @Test
    void transition_requestedConfirm_movesToConfirmed() {
        assertThat(AppointmentStateMachine.transition(REQUESTED, CONFIRM)).isEqualTo(CONFIRMED);
    }

    @Test
    void transition_requestedCancel_movesToCancelled() {
        assertThat(AppointmentStateMachine.transition(REQUESTED, CANCEL)).isEqualTo(CANCELLED);
    }

    @Test
    void transition_confirmedAttend_movesToAttended() {
        assertThat(AppointmentStateMachine.transition(CONFIRMED, ATTEND)).isEqualTo(ATTENDED);
    }

    @Test
    void transition_confirmedCancel_movesToCancelled() {
        assertThat(AppointmentStateMachine.transition(CONFIRMED, CANCEL)).isEqualTo(CANCELLED);
    }

    @Test
    void transition_confirmedMarkNoShow_movesToNoShow() {
        assertThat(AppointmentStateMachine.transition(CONFIRMED, MARK_NO_SHOW)).isEqualTo(NO_SHOW);
    }

    @ParameterizedTest(name = "{index}: {0} + {1} is invalid")
    @MethodSource("invalidStateEventPairs")
    void transition_invalidPair_throwsIllegalStateException(AppointmentStatus state, AppointmentEvent event) {
        assertThatThrownBy(() -> AppointmentStateMachine.transition(state, event))
                .isInstanceOf(IllegalStateException.class);
    }

    /**
     * All 20 state/event pairs minus the 5 valid ones from CLAUDE.md's
     * table, generated rather than hand-listed so the count (15) is
     * enforced by the grid itself, not by hand-copying.
     */
    static Stream<Arguments> invalidStateEventPairs() {
        record Valid(AppointmentStatus state, AppointmentEvent event) {
        }
        var validPairs = Stream.of(
                new Valid(REQUESTED, CONFIRM),
                new Valid(REQUESTED, CANCEL),
                new Valid(CONFIRMED, ATTEND),
                new Valid(CONFIRMED, CANCEL),
                new Valid(CONFIRMED, MARK_NO_SHOW)
        ).toList();

        return EnumSet.allOf(AppointmentStatus.class).stream()
                .flatMap(state -> EnumSet.allOf(AppointmentEvent.class).stream()
                        .filter(event -> validPairs.stream()
                                .noneMatch(v -> v.state() == state && v.event() == event))
                        .map(event -> Arguments.of(state, event)));
    }

    @Test
    void invalidStateEventPairs_containsExactlyFifteenPairs() {
        assertThat(invalidStateEventPairs().count()).isEqualTo(15);
    }

    @Test
    void lateCancellationFee_atTwentyThreeHours59Minutes_chargesHalfFee() {
        LocalDateTime now = SLOT_START.minusHours(23).minusMinutes(59);

        BigDecimal fee = AppointmentStateMachine.lateCancellationFee(now, SLOT_START, new BigDecimal("250"));

        assertThat(fee).isEqualByComparingTo(new BigDecimal("125.0"));
    }

    @Test
    void lateCancellationFee_atExactlyTwentyFourHours_isFree() {
        LocalDateTime now = SLOT_START.minusHours(24);

        BigDecimal fee = AppointmentStateMachine.lateCancellationFee(now, SLOT_START, new BigDecimal("250"));

        assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void lateCancellationFee_atTwentyFourHoursOneMinute_isFree() {
        LocalDateTime now = SLOT_START.minusHours(24).minusMinutes(1);

        BigDecimal fee = AppointmentStateMachine.lateCancellationFee(now, SLOT_START, new BigDecimal("250"));

        assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
