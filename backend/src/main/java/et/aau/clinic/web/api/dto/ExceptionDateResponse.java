package et.aau.clinic.web.api.dto;

import et.aau.clinic.domain.AvailabilityException;

import java.time.LocalDate;

public record ExceptionDateResponse(Long id, LocalDate date) {

    public static ExceptionDateResponse from(AvailabilityException exception) {
        return new ExceptionDateResponse(exception.getId(), exception.getDate());
    }
}
