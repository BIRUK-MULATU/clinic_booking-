package et.aau.clinic.web.api;

import et.aau.clinic.service.AppointmentService;
import et.aau.clinic.web.api.dto.AppointmentResponse;
import et.aau.clinic.web.api.dto.BookingRequest;
import et.aau.clinic.web.api.dto.ErrorResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Hospital-expansion Phase C, surfaced through the JSON API. */
@RestController
public class WaitlistApiController {

    private final AppointmentService appointmentService;

    public WaitlistApiController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping("/api/waitlist")
    public ResponseEntity<?> joinWaitlist(@RequestBody BookingRequest request, HttpSession session) {
        Long patientId = (Long) session.getAttribute("patientId");
        if (patientId == null) {
            return ResponseEntity.status(401).body(new ErrorResponse("Not logged in."));
        }
        var appointment = appointmentService.joinWaitlist(patientId, request.slotId());
        return ResponseEntity.status(201).body(AppointmentResponse.from(appointment));
    }
}
