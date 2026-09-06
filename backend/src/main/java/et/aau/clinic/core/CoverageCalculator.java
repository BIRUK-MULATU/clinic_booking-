package et.aau.clinic.core;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Rule I (hospital-expansion): insurance coverage. A patient carries a
 * coverage percentage; the clinic bills them the remainder of the
 * consultation fee after that percentage is waived.
 *
 *   net payable = fee - (fee * coveragePercent / 100)
 *
 * A pure calculation with one validated input range, the same shape as
 * FeeCalculator (Rule 1): a percentage outside 0-100 has no meaning, so
 * it is rejected outright rather than clamped or allowed to produce a
 * negative bill or a bill larger than the fee.
 *
 * Boundary value target: the valid range is the closed interval
 * [0, 100], so the boundaries to test are -1, 0, 1, 99, 100 and 101.
 * Equivalence partitions: invalid-low (< 0), valid (0-100), invalid-high
 * (> 100).
 */
public final class CoverageCalculator {

    static final int MIN_PERCENT = 0;
    static final int MAX_PERCENT = 100;

    private CoverageCalculator() {
    }

    public static BigDecimal netPayable(BigDecimal consultationFee, int coveragePercent) {
        if (coveragePercent < MIN_PERCENT || coveragePercent > MAX_PERCENT) {
            throw new IllegalArgumentException(
                    "Coverage percent must be between 0 and 100, was " + coveragePercent);
        }
        BigDecimal waived = consultationFee
                .multiply(BigDecimal.valueOf(coveragePercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return consultationFee.subtract(waived).setScale(2, RoundingMode.HALF_UP);
    }
}
