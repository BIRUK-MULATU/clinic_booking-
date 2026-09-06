package et.aau.clinic.web.api.dto;

/** Setting a doctor's daily patient limit. */
public record DoctorLimitRequest(Integer dailyPatientLimit) {
}
