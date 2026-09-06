package et.aau.clinic.service;

import et.aau.clinic.domain.Appointment;
import et.aau.clinic.domain.Patient;

/**
 * Kept deliberately tiny: this is the seam unit tests mock to verify a
 * message was sent without actually sending an SMS. The second method
 * is the 24-hour reminder (hospital-expansion Rule H) - the scheduled
 * job calls it, and tests verify it the same way they verify
 * sendConfirmation.
 */
public interface NotificationService {

    void sendConfirmation(Patient patient, Appointment appointment);

    void sendReminder(Patient patient, Appointment appointment);
}
