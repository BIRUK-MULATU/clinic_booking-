package et.aau.clinic.web;

import et.aau.clinic.service.AppointmentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class MyAppointmentsController {

    private final AppointmentService appointmentService;

    public MyAppointmentsController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/my-appointments")
    public String myAppointments(HttpSession session, Model model) {
        Long patientId = (Long) session.getAttribute("patientId");
        model.addAttribute("appointments", appointmentService.listAppointmentsForPatient(patientId));
        return "my-appointments";
    }

    @PostMapping("/appointments/{id}/confirm")
    public String confirm(@PathVariable Long id) {
        appointmentService.confirm(id);
        return "redirect:/my-appointments";
    }

    @PostMapping("/appointments/{id}/cancel")
    public String cancel(@PathVariable Long id) {
        appointmentService.cancel(id);
        return "redirect:/my-appointments";
    }

    @PostMapping("/appointments/{id}/attend")
    public String attend(@PathVariable Long id) {
        appointmentService.markAttended(id);
        return "redirect:/my-appointments";
    }

    @PostMapping("/appointments/{id}/no-show")
    public String noShow(@PathVariable Long id) {
        appointmentService.markNoShow(id);
        return "redirect:/my-appointments";
    }
}
