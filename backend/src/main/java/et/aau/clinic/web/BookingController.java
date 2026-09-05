package et.aau.clinic.web;

import et.aau.clinic.service.AppointmentService;
import et.aau.clinic.service.BookingOutcome;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class BookingController {

    private final AppointmentService appointmentService;

    public BookingController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/book/{slotId}")
    public String bookingForm(@PathVariable Long slotId, Model model) {
        model.addAttribute("slot", appointmentService.getSlot(slotId));
        return "book";
    }

    @PostMapping("/book")
    public String submitBooking(@RequestParam Long slotId, HttpSession session, Model model) {
        Long patientId = (Long) session.getAttribute("patientId");
        BookingOutcome outcome = appointmentService.requestBooking(patientId, slotId);

        if (!outcome.decision().isApproved()) {
            model.addAttribute("error", outcome.decision().getReason());
            model.addAttribute("slot", appointmentService.getSlot(slotId));
            return "book";
        }

        return "redirect:/confirmation/" + outcome.appointment().getId();
    }

    @GetMapping("/confirmation/{appointmentId}")
    public String confirmation(@PathVariable Long appointmentId, Model model) {
        model.addAttribute("appointment", appointmentService.getAppointment(appointmentId));
        return "confirmation";
    }
}
