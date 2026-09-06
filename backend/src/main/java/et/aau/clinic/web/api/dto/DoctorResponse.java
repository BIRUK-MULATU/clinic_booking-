package et.aau.clinic.web.api.dto;

import et.aau.clinic.core.DoctorLoad;
import et.aau.clinic.domain.Doctor;
import et.aau.clinic.domain.DoctorAvailability;

import java.util.List;

/**
 * A doctor for the frontend. photo is the data URL stored on the entity
 * (null when none uploaded). availability and todayLoad are populated
 * only by the doctors-list endpoint, which has the data to hand;
 * everywhere else (e.g. inside a SlotResponse) they are empty/null.
 */
public record DoctorResponse(Long id, String name, String specialty, String departmentName, String photo,
                             int dailyPatientLimit, List<AvailabilityRuleResponse> availability,
                             DoctorLoadResponse todayLoad) {

    public static DoctorResponse from(Doctor doctor) {
        return from(doctor, List.of(), null);
    }

    public static DoctorResponse from(Doctor doctor, List<DoctorAvailability> rules, DoctorLoad todayLoad) {
        return new DoctorResponse(
                doctor.getId(),
                doctor.getName(),
                doctor.getSpecialty(),
                doctor.getDepartment().getName(),
                doctor.getPhoto(),
                doctor.getDailyPatientLimit(),
                rules.stream().map(AvailabilityRuleResponse::from).toList(),
                todayLoad == null ? null : DoctorLoadResponse.from(todayLoad));
    }
}
