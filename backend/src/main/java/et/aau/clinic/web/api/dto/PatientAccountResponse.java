package et.aau.clinic.web.api.dto;

import et.aau.clinic.domain.Patient;

import java.math.BigDecimal;

/**
 * A patient account as the admin sees it. Deliberately does NOT echo the
 * password back - the admin typed it, they can note it down; sending it
 * back in every list response is needless exposure even in a demo.
 */
public record PatientAccountResponse(Long id, String name, String dateOfBirth, String phone, String username,
                                     BigDecimal outstandingBalance) {

    public static PatientAccountResponse from(Patient patient) {
        return new PatientAccountResponse(
                patient.getId(),
                patient.getName(),
                patient.getDateOfBirth().toString(),
                patient.getPhone(),
                patient.getUsername(),
                patient.getOutstandingBalance());
    }
}
