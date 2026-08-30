package et.aau.clinic.core;

import et.aau.clinic.domain.FeeCategory;

import java.math.BigDecimal;

/**
 * Rule 1: consultation fee by age. A pure lookup with three closed bands;
 * ages outside 0-120 have no band, so they are rejected outright rather
 * than silently falling into one of the three.
 */
public final class FeeCalculator {

    private static final BigDecimal CHILD_FEE = new BigDecimal("100");
    private static final BigDecimal ADULT_FEE = new BigDecimal("250");
    private static final BigDecimal SENIOR_FEE = new BigDecimal("150");

    private FeeCalculator() {
    }

    public static Fee calculate(int age) {
        if (age < 0 || age > 120) {
            throw new IllegalArgumentException("Age must be between 0 and 120, was " + age);
        }
        if (age <= 17) {
            return new Fee(FeeCategory.CHILD, CHILD_FEE);
        }
        if (age <= 64) {
            return new Fee(FeeCategory.ADULT, ADULT_FEE);
        }
        return new Fee(FeeCategory.SENIOR, SENIOR_FEE);
    }
}
