package et.aau.clinic.domain;

/**
 * How full a doctor's day is against their daily patient limit
 * (hospital-expansion: doctor capacity). AVAILABLE = room to spare,
 * NEARLY_FULL = exactly one place left, FULL = at or over the limit.
 */
public enum DoctorLoadStatus {
    AVAILABLE,
    NEARLY_FULL,
    FULL
}
