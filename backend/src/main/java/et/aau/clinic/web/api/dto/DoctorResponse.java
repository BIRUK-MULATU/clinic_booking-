package et.aau.clinic.web.api.dto;

import et.aau.clinic.domain.Doctor;
import et.aau.clinic.domain.DoctorAvailability;

import java.util.List;

/**
 * A doctor for the frontend. photo is the data URL stored on the entity
 * (null when none uploaded). availability is populated only by the
 * doctors-list endpoint, which has the rules to hand; everywhere else
 * (e.g. inside a SlotResponse) it is an empty list.
 */
public record DoctorResponse(Long id, String name, String specialty, String departmentName, String photo,
                             List<AvailabilityRuleResponse> availability) {

    public static DoctorResponse from(Doctor doctor) {
        return from(doctor, List.of());
    }

    public static DoctorResponse from(Doctor doctor, List<DoctorAvailability> rules) {
        return new DoctorResponse(
                doctor.getId(),
                doctor.getName(),
                doctor.getSpecialty(),
                doctor.getDepartment().getName(),
                doctor.getPhoto(),
                rules.stream().map(AvailabilityRuleResponse::from).toList());
    }
}
