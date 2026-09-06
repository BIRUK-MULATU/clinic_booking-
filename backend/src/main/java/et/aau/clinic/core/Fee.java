package et.aau.clinic.core;

import et.aau.clinic.domain.FeeCategory;

import java.math.BigDecimal;

/**
 * The result of FeeCalculator: which age band a patient falls into,
 * and the ETB amount that band charges.
 */
public record Fee(FeeCategory category, BigDecimal amount) {
}
