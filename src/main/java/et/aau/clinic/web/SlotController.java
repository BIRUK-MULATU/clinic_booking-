package et.aau.clinic.web;

import et.aau.clinic.service.AppointmentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SlotController {

    private final AppointmentService appointmentService;

    public SlotController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/slots")
    public String listSlots(HttpSession session, Model model) {
        model.addAttribute("slots", appointmentService.listAvailableSlots());
        model.addAttribute("patientName", session.getAttribute("patientName"));
        return "slots";
    }
}
