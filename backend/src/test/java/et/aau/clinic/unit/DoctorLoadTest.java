package et.aau.clinic.unit;

import et.aau.clinic.core.DoctorLoad;
import et.aau.clinic.domain.DoctorLoadStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rule G (hospital-expansion: doctor capacity) - equivalence
 * partitioning over the three load bands (AVAILABLE / NEARLY_FULL /
 * FULL) plus boundary value analysis around the daily limit.
 *
 * Partitions, for a daily limit L: scheduled <= L-2 is AVAILABLE,
 * scheduled == L-1 is NEARLY_FULL, scheduled >= L is FULL. The
 * boundaries tested are L-2, L-1, L and L+1, with L fixed at 4 (the
 * value from the user's example), plus the L=1 edge case and an
 * over-booked day.
 */
class DoctorLoadTest {

    // TC-G01 - EP: AVAILABLE partition interior (limit 4, nobody booked).
    @Test
    void load_noneScheduled_isAvailableWithFullRemaining() {
        DoctorLoad load = DoctorLoad.evaluate(4, 0);
        assertThat(load.status()).isEqualTo(DoctorLoadStatus.AVAILABLE);
        assertThat(load.remaining()).isEqualTo(4);
    }

    // TC-G02 - BVA: L-2 (two places left) is still AVAILABLE.
    @Test
    void load_twoBelowLimit_isAvailable() {
        assertThat(DoctorLoad.evaluate(4, 2).status()).isEqualTo(DoctorLoadStatus.AVAILABLE);
    }

    // TC-G03 - BVA: L-1 (exactly one place left) is NEARLY_FULL.
    @Test
    void load_oneBelowLimit_isNearlyFull() {
        DoctorLoad load = DoctorLoad.evaluate(4, 3);
        assertThat(load.status()).isEqualTo(DoctorLoadStatus.NEARLY_FULL);
        assertThat(load.remaining()).isEqualTo(1);
    }

    // TC-G04 - BVA: scheduled == L is FULL, remaining 0.
    @Test
    void load_atLimit_isFull() {
        DoctorLoad load = DoctorLoad.evaluate(4, 4);
        assertThat(load.status()).isEqualTo(DoctorLoadStatus.FULL);
        assertThat(load.remaining()).isZero();
        assertThat(load.isOverLimit()).isFalse();
    }

    // TC-G05 - BVA: scheduled == L+1 is FULL and flagged as over the limit.
    @Test
    void load_oneOverLimit_isFullAndOverLimit() {
        DoctorLoad load = DoctorLoad.evaluate(4, 5);
        assertThat(load.status()).isEqualTo(DoctorLoadStatus.FULL);
        assertThat(load.remaining()).isZero();
        assertThat(load.isOverLimit()).isTrue();
    }

    // TC-G06 - EP: FULL partition interior, well over the limit.
    @Test
    void load_wellOverLimit_isFull() {
        assertThat(DoctorLoad.evaluate(4, 9).status()).isEqualTo(DoctorLoadStatus.FULL);
    }

    // TC-G07 - Edge: a limit of 1 - zero booked is the last place (NEARLY_FULL)...
    @Test
    void load_limitOne_noneScheduled_isNearlyFull() {
        assertThat(DoctorLoad.evaluate(1, 0).status()).isEqualTo(DoctorLoadStatus.NEARLY_FULL);
    }

    // TC-G08 - ...and one booked fills it.
    @Test
    void load_limitOne_oneScheduled_isFull() {
        assertThat(DoctorLoad.evaluate(1, 1).status()).isEqualTo(DoctorLoadStatus.FULL);
    }
}
