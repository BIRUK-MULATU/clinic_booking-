package et.aau.clinic.web.api;

import et.aau.clinic.service.AppointmentService;
import et.aau.clinic.service.BookingOutcome;
import et.aau.clinic.web.api.dto.AppointmentResponse;
import et.aau.clinic.web.api.dto.BookingRequest;
import et.aau.clinic.web.api.dto.BookingResponse;
import et.aau.clinic.web.api.dto.ErrorResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookingApiController {

    private final AppointmentService appointmentService;

    public BookingApiController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping("/api/bookings")
    public ResponseEntity<?> book(@RequestBody BookingRequest request, HttpSession session) {
        Long patientId = (Long) session.getAttribute("patientId");
        if (patientId == null) {
            return ResponseEntity.status(401).body(new ErrorResponse("Not logged in."));
        }

        BookingOutcome outcome = appointmentService.requestBooking(patientId, request.slotId());
        if (!outcome.decision().isApproved()) {
            return ResponseEntity.ok(new BookingResponse(false, outcome.decision().getReason().name(), null));
        }
        return ResponseEntity.status(201)
                .body(new BookingResponse(true, null, AppointmentResponse.from(outcome.appointment())));
    }
}
