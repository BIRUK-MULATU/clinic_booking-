package et.aau.clinic.unit;

import et.aau.clinic.core.Fee;
import et.aau.clinic.core.FeeCalculator;
import et.aau.clinic.domain.FeeCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Rule 1 - equivalence partitioning and boundary value analysis.
 *
 * Partitions: invalid-low (age < 0), CHILD (0-17), ADULT (18-64),
 * SENIOR (65-120), invalid-high (age > 120). Every boundary listed in
 * CLAUDE.md (-1, 0, 17, 18, 64, 65, 120, 121) has its own test, plus
 * one interior representative per valid partition and two far-out
 * invalid values to confirm the invalid partitions aren't accidentally
 * bounded only at their near edge.
 */
class FeeCalculatorTest {

    @Test
    void fee_belowLowerBoundary_ageMinus1_throws() {
        assertThatThrownBy(() -> FeeCalculator.calculate(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fee_wellBelowRange_ageMinus50_throws() {
        assertThatThrownBy(() -> FeeCalculator.calculate(-50))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fee_atLowerBoundaryOfChildBand_age0_is100() {
        assertFee(0, FeeCategory.CHILD, "100");
    }

    @Test
    void fee_withinChildBand_age9_is100() {
        assertFee(9, FeeCategory.CHILD, "100");
    }

    @Test
    void fee_atUpperBoundaryOfChildBand_age17_is100() {
        assertFee(17, FeeCategory.CHILD, "100");
    }

    @Test
    void fee_atLowerBoundaryOfAdultBand_age18_is250() {
        assertFee(18, FeeCategory.ADULT, "250");
    }

    @Test
    void fee_withinAdultBand_age40_is250() {
        assertFee(40, FeeCategory.ADULT, "250");
    }

    @Test
    void fee_atUpperBoundaryOfAdultBand_age64_is250() {
        assertFee(64, FeeCategory.ADULT, "250");
    }

    @Test
    void fee_atLowerBoundaryOfSeniorBand_age65_is150() {
        assertFee(65, FeeCategory.SENIOR, "150");
    }

    @Test
    void fee_withinSeniorBand_age90_is150() {
        assertFee(90, FeeCategory.SENIOR, "150");
    }

    @Test
    void fee_atUpperBoundaryOfSeniorBand_age120_is150() {
        assertFee(120, FeeCategory.SENIOR, "150");
    }

    @Test
    void fee_aboveUpperBoundary_age121_throws() {
        assertThatThrownBy(() -> FeeCalculator.calculate(121))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fee_wellAboveRange_age200_throws() {
        assertThatThrownBy(() -> FeeCalculator.calculate(200))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void assertFee(int age, FeeCategory expectedCategory, String expectedAmount) {
        Fee fee = FeeCalculator.calculate(age);
        assertThat(fee.category()).isEqualTo(expectedCategory);
        assertThat(fee.amount()).isEqualByComparingTo(new BigDecimal(expectedAmount));
    }
}
