package et.aau.clinic.web.api.dto;

import et.aau.clinic.domain.Appointment;

import java.math.BigDecimal;

/**
 * Like AppointmentResponse, but for reception's day roster - includes
 * the patient's name, which a patient never needs to see about their
 * own appointment but reception needs to see about everyone's.
 */
public record AdminAppointmentResponse(Long id, String patientName, String slotStartTime, String status,
                                        String feeCategory, BigDecimal feeAmount) {

    public static AdminAppointmentResponse from(Appointment appointment) {
        return new AdminAppointmentResponse(
                appointment.getId(),
                appointment.getPatient().getName(),
                appointment.getSlot().getStartTime().toString(),
                appointment.getStatus().name(),
                appointment.getFeeCategory().name(),
                appointment.getFeeAmount());
    }
}
