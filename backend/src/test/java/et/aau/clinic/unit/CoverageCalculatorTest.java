package et.aau.clinic.unit;

import et.aau.clinic.core.CoverageCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Rule G (hospital-expansion) - insurance coverage. Equivalence
 * partitioning on the coverage percentage (invalid-low &lt; 0, valid
 * 0-100, invalid-high &gt; 100) and boundary value analysis on the
 * closed valid range [0, 100]: the six values -1, 0, 1, 99, 100, 101.
 *
 * A 250 ETB adult fee is used throughout so the arithmetic is easy to
 * check by eye: 20% coverage waives 50, leaving 200.
 */
class CoverageCalculatorTest {

    private static final BigDecimal ADULT_FEE = new BigDecimal("250");

    // TC-G01 - EP invalid-low / BVA -1: just below the range is rejected.
    @Test
    void coverage_minusOnePercent_isRejected() {
        assertThatThrownBy(() -> CoverageCalculator.netPayable(ADULT_FEE, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // TC-G02 - BVA 0: the lower boundary. No coverage means the full fee is payable.
    @Test
    void coverage_zeroPercent_paysFullFee() {
        assertThat(CoverageCalculator.netPayable(ADULT_FEE, 0)).isEqualByComparingTo("250.00");
    }

    // TC-G03 - BVA 1: just inside the lower boundary.
    @Test
    void coverage_onePercent_waivesOnePercent() {
        // 1% of 250 = 2.50, leaving 247.50
        assertThat(CoverageCalculator.netPayable(ADULT_FEE, 1)).isEqualByComparingTo("247.50");
    }

    // TC-G04 - EP valid (mid-partition): a representative interior value.
    @Test
    void coverage_twentyPercent_waivesTwentyPercent() {
        assertThat(CoverageCalculator.netPayable(ADULT_FEE, 20)).isEqualByComparingTo("200.00");
    }

    // TC-G05 - BVA 99: just inside the upper boundary.
    @Test
    void coverage_ninetyNinePercent_waivesNearlyEverything() {
        // 99% of 250 = 247.50, leaving 2.50
        assertThat(CoverageCalculator.netPayable(ADULT_FEE, 99)).isEqualByComparingTo("2.50");
    }

    // TC-G06 - BVA 100: the upper boundary. Full coverage means nothing is payable.
    @Test
    void coverage_hundredPercent_paysNothing() {
        assertThat(CoverageCalculator.netPayable(ADULT_FEE, 100)).isEqualByComparingTo("0.00");
    }

    // TC-G07 - EP invalid-high / BVA 101: just above the range is rejected.
    @Test
    void coverage_hundredAndOnePercent_isRejected() {
        assertThatThrownBy(() -> CoverageCalculator.netPayable(ADULT_FEE, 101))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // TC-G08 - EP invalid-high (further in the partition), to show it is the whole partition
    // that is rejected, not just the 101 boundary.
    @Test
    void coverage_wayOverAHundredPercent_isRejected() {
        assertThatThrownBy(() -> CoverageCalculator.netPayable(ADULT_FEE, 500))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // TC-G09 - rounding: a fee/percent pair that does not divide evenly is rounded to 2dp.
    @Test
    void coverage_thirtyThreePercentOfOneHundred_isRoundedToTwoDecimals() {
        // 33% of 100 = 33.00 waived, leaving 67.00 - and the intermediate divide is half-up
        assertThat(CoverageCalculator.netPayable(new BigDecimal("100"), 33)).isEqualByComparingTo("67.00");
    }
}
