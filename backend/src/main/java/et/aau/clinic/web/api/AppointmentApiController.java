package et.aau.clinic.web.api;

import et.aau.clinic.service.AppointmentService;
import et.aau.clinic.web.api.dto.AppointmentResponse;
import et.aau.clinic.web.api.dto.ErrorResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppointmentApiController {

    private final AppointmentService appointmentService;

    public AppointmentApiController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/api/appointments")
    public ResponseEntity<?> myAppointments(HttpSession session) {
        Long patientId = (Long) session.getAttribute("patientId");
        if (patientId == null) {
            return ResponseEntity.status(401).body(new ErrorResponse("Not logged in."));
        }
        return ResponseEntity.ok(appointmentService.listAppointmentsForPatient(patientId).stream()
                .map(AppointmentResponse::from)
                .toList());
    }

    @GetMapping("/api/appointments/{id}")
    public ResponseEntity<?> getAppointment(@PathVariable Long id, HttpSession session) {
        if (session.getAttribute("patientId") == null) {
            return ResponseEntity.status(401).body(new ErrorResponse("Not logged in."));
        }
        return ResponseEntity.ok(AppointmentResponse.from(appointmentService.getAppointment(id)));
    }

    // Reception-only: a patient requests a booking, but the clinic (reception) is who
    // actually confirms it - matching how a real front desk works, not a self-service toggle.
    @PostMapping("/api/appointments/{id}/confirm")
    public ResponseEntity<?> confirm(@PathVariable Long id, HttpSession session) {
        if (session.getAttribute("patientId") == null) {
            return ResponseEntity.status(401).body(new ErrorResponse("Not logged in."));
        }
        if (!"ADMIN".equals(session.getAttribute("role"))) {
            return ResponseEntity.status(403).body(new ErrorResponse("Only reception can confirm an appointment."));
        }
        return ResponseEntity.ok(AppointmentResponse.from(appointmentService.confirm(id)));
    }

    @PostMapping("/api/appointments/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id, HttpSession session) {
        if (session.getAttribute("patientId") == null) {
            return ResponseEntity.status(401).body(new ErrorResponse("Not logged in."));
        }
        return ResponseEntity.ok(AppointmentResponse.from(appointmentService.cancel(id)));
    }
}
