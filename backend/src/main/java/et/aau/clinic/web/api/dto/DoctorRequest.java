package et.aau.clinic.web.api.dto;

public record DoctorRequest(String name, String specialty, Long departmentId) {
}
