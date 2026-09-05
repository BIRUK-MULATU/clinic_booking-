package et.aau.clinic.web.api.dto;

/** photo is an optional data URL uploaded from the admin's device. */
public record DoctorRequest(String name, String specialty, Long departmentId, String photo) {
}
