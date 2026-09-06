package et.aau.clinic.web.api.dto;

import et.aau.clinic.domain.Appointment;

/**
 * One row of reception's "reminders due" list (hospital-expansion Rule
 * F): a CONFIRMED appointment coming up within 24 hours, with the
 * patient's name and phone so the admin knows who is being texted, and
 * reminderSentAt (null until it has gone out) so a sent one shows as
 * done rather than offering the button again.
 */
public record ReminderResponse(Long id, String patientName, String phone, String slotStartTime,
                                String reminderSentAt) {

    public static ReminderResponse from(Appointment appointment) {
        return new ReminderResponse(
                appointment.getId(),
                appointment.getPatient().getName(),
                appointment.getPatient().getPhone(),
                appointment.getSlot().getStartTime().toString(),
                appointment.getReminderSentAt() != null ? appointment.getReminderSentAt().toString() : null);
    }
}
