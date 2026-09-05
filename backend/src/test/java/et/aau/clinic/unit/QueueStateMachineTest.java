package et.aau.clinic.unit;

import et.aau.clinic.core.QueueEvent;
import et.aau.clinic.core.QueueStateMachine;
import et.aau.clinic.domain.QueueEntryStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.EnumSet;
import java.util.stream.Stream;

import static et.aau.clinic.core.QueueEvent.CALL;
import static et.aau.clinic.core.QueueEvent.COMPLETE;
import static et.aau.clinic.core.QueueEvent.START_CONSULTATION;
import static et.aau.clinic.domain.QueueEntryStatus.CALLED;
import static et.aau.clinic.domain.QueueEntryStatus.DONE;
import static et.aau.clinic.domain.QueueEntryStatus.IN_CONSULTATION;
import static et.aau.clinic.domain.QueueEntryStatus.WAITING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Hospital-expansion Phase D - state transition testing over the queue
 * lifecycle's 4 states x 3 events grid (12 pairs: 3 valid, 9 invalid),
 * same generated-not-hand-listed approach as AppointmentStateMachineTest.
 */
class QueueStateMachineTest {

    // TC-Q01 - State transition table: WAITING --call--> CALLED (valid).
    @Test
    void transition_waitingCall_movesToCalled() {
        assertThat(QueueStateMachine.transition(WAITING, CALL)).isEqualTo(CALLED);
    }

    // TC-Q02 - State transition table: CALLED --startConsultation--> IN_CONSULTATION (valid).
    @Test
    void transition_calledStartConsultation_movesToInConsultation() {
        assertThat(QueueStateMachine.transition(CALLED, START_CONSULTATION)).isEqualTo(IN_CONSULTATION);
    }

    // TC-Q03 - State transition table: IN_CONSULTATION --complete--> DONE (valid).
    @Test
    void transition_inConsultationComplete_movesToDone() {
        assertThat(QueueStateMachine.transition(IN_CONSULTATION, COMPLETE)).isEqualTo(DONE);
    }

    // State transition table: covers all 9 invalid (state, event) pairs - the full 4x3 grid
    // (12 pairs) minus the 3 valid transitions TC-Q01-TC-Q03 cover above.
    @ParameterizedTest(name = "{index}: {0} + {1} is invalid")
    @MethodSource("invalidStateEventPairs")
    void transition_invalidPair_throwsIllegalStateException(QueueEntryStatus state, QueueEvent event) {
        assertThatThrownBy(() -> QueueStateMachine.transition(state, event))
                .isInstanceOf(IllegalStateException.class);
    }

    /**
     * State transition table: data source for the 9 invalid pairs above. All 12
     * state/event pairs minus the 3 valid ones, generated rather than hand-listed for the
     * same reason as AppointmentStateMachineTest's equivalent method.
     */
    static Stream<Arguments> invalidStateEventPairs() {
        record Valid(QueueEntryStatus state, QueueEvent event) {
        }
        var validPairs = Stream.of(
                new Valid(WAITING, CALL),
                new Valid(CALLED, START_CONSULTATION),
                new Valid(IN_CONSULTATION, COMPLETE)
        ).toList();

        return EnumSet.allOf(QueueEntryStatus.class).stream()
                .flatMap(state -> EnumSet.allOf(QueueEvent.class).stream()
                        .filter(event -> validPairs.stream()
                                .noneMatch(v -> v.state() == state && v.event() == event))
                        .map(event -> Arguments.of(state, event)));
    }

    // Meta-check on the state transition table itself: 4 states x 3 events = 12 pairs,
    // 3 valid, so 9 invalid (12 - 3 = 9).
    @Test
    void invalidStateEventPairs_containsExactlyNinePairs() {
        assertThat(invalidStateEventPairs().count()).isEqualTo(9);
    }
}
