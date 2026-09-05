package et.aau.clinic.web.api.dto;

import et.aau.clinic.domain.Appointment;

import java.math.BigDecimal;

public record AppointmentResponse(
        Long id,
        Long slotId,
        String slotStartTime,
        String status,
        String feeCategory,
        BigDecimal feeAmount,
        BigDecimal cancellationFee) {

    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getSlot().getId(),
                appointment.getSlot().getStartTime().toString(),
                appointment.getStatus().name(),
                appointment.getFeeCategory().name(),
                appointment.getFeeAmount(),
                appointment.getCancellationFee());
    }
}
