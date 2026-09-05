package et.aau.clinic.web;

import et.aau.clinic.domain.Patient;
import et.aau.clinic.service.AppointmentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class LoginController {

    private final AppointmentService appointmentService;

    public LoginController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password,
                         HttpSession session, Model model) {
        Optional<Patient> patient = appointmentService.login(username, password);
        if (patient.isEmpty()) {
            model.addAttribute("error", "Invalid username or password.");
            return "login";
        }
        session.setAttribute("patientId", patient.get().getId());
        session.setAttribute("patientName", patient.get().getName());
        return "redirect:/slots";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
