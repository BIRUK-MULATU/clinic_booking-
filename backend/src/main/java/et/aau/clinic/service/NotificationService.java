package et.aau.clinic.service;

import et.aau.clinic.domain.Appointment;
import et.aau.clinic.domain.Patient;

/**
 * Kept to a single method deliberately: this is the seam unit tests
 * mock to verify a confirmation was sent without actually sending an
 * SMS. A bigger interface would only give Mockito more to stub.
 */
public interface NotificationService {

    void sendConfirmation(Patient patient, Appointment appointment);
}
