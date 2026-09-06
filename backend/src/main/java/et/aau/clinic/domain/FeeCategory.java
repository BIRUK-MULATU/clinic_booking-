package et.aau.clinic.domain;

/**
 * The three age bands from Rule 1. An age outside 0-120 falls into none
 * of these and is rejected by FeeCalculator before a category is chosen.
 */
public enum FeeCategory {
    CHILD,
    ADULT,
    SENIOR
}
