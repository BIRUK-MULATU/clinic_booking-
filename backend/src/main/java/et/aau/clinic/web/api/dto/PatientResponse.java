package et.aau.clinic.web.api.dto;

import et.aau.clinic.domain.Patient;

public record PatientResponse(Long id, String name, String role) {

    public static PatientResponse from(Patient patient) {
        return new PatientResponse(patient.getId(), patient.getName(), patient.getRole().name());
    }
}
