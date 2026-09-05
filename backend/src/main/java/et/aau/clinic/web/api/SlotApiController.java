package et.aau.clinic.web.api;

import et.aau.clinic.service.AppointmentService;
import et.aau.clinic.web.api.dto.ErrorResponse;
import et.aau.clinic.web.api.dto.SlotResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SlotApiController {

    private final AppointmentService appointmentService;

    public SlotApiController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/api/slots")
    public ResponseEntity<?> listSlots(HttpSession session) {
        if (session.getAttribute("patientId") == null) {
            return ResponseEntity.status(401).body(new ErrorResponse("Not logged in."));
        }
        return ResponseEntity.ok(appointmentService.listAvailableSlots().stream()
                .map(SlotResponse::from)
                .toList());
    }

    @GetMapping("/api/slots/{id}")
    public ResponseEntity<?> getSlot(@PathVariable Long id, HttpSession session) {
        if (session.getAttribute("patientId") == null) {
            return ResponseEntity.status(401).body(new ErrorResponse("Not logged in."));
        }
        return ResponseEntity.ok(SlotResponse.from(appointmentService.getSlot(id)));
    }
}
