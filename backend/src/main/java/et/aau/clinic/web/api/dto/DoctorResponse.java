package et.aau.clinic.web.api.dto;

import et.aau.clinic.domain.Doctor;

public record DoctorResponse(Long id, String name, String specialty, String departmentName) {

    public static DoctorResponse from(Doctor doctor) {
        return new DoctorResponse(doctor.getId(), doctor.getName(), doctor.getSpecialty(),
                doctor.getDepartment().getName());
    }
}
