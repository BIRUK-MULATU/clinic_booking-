package et.aau.clinic.web.api;

import et.aau.clinic.domain.Patient;
import et.aau.clinic.service.AppointmentService;
import et.aau.clinic.web.api.dto.ErrorResponse;
import et.aau.clinic.web.api.dto.LoginRequest;
import et.aau.clinic.web.api.dto.PatientResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * JSON login/session endpoints for the React frontend (frontend/). Mirrors
 * LoginController's session-based approach exactly - same HttpSession
 * attributes ("patientId", "patientName") - so a request can freely go
 * through either the Thymeleaf pages or this API in the same browser
 * session. Not part of the graded Thymeleaf app and has no dedicated
 * tests of its own; the login logic it calls (AppointmentService.login)
 * is already covered by AppointmentServiceTest.
 */
@RestController
public class AuthApiController {

    private final AppointmentService appointmentService;

    public AuthApiController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping("/api/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpSession session) {
        Optional<Patient> patient = appointmentService.login(request.username(), request.password());
        if (patient.isEmpty()) {
            return ResponseEntity.status(401).body(new ErrorResponse("Invalid username or password."));
        }
        session.setAttribute("patientId", patient.get().getId());
        session.setAttribute("patientName", patient.get().getName());
        session.setAttribute("role", patient.get().getRole().name());
        return ResponseEntity.ok(PatientResponse.from(patient.get()));
    }

    @PostMapping("/api/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/session")
    public ResponseEntity<?> session(HttpSession session) {
        Long patientId = (Long) session.getAttribute("patientId");
        String patientName = (String) session.getAttribute("patientName");
        String role = (String) session.getAttribute("role");
        if (patientId == null) {
            return ResponseEntity.status(401).body(new ErrorResponse("Not logged in."));
        }
        return ResponseEntity.ok(new PatientResponse(patientId, patientName, role));
    }
}
