package et.aau.clinic.integration;

import et.aau.clinic.domain.Patient;
import et.aau.clinic.domain.Role;
import et.aau.clinic.domain.Slot;
import et.aau.clinic.repository.PatientRepository;
import et.aau.clinic.repository.SlotRepository;
import et.aau.clinic.service.AppointmentService;
import et.aau.clinic.service.BookingOutcome;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Hospital-expansion Phase E, through the JSON API: VisitRecordApiController
 * driven by real HTTP requests (MockMvc) against the real controller,
 * session, service and H2 - the API-layer counterpart to
 * VisitRecordServiceIT. Covers the three things only the web layer adds
 * on top of the service: the ADMIN-only guard on POST, the 200 +
 * recorded=false shape for a policy rejection, and the 201 + body on
 * success.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VisitRecordApiIT {

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 1, 10, 11, 30);

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(FIXED_NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private SlotRepository slotRepository;
    @Autowired
    private AppointmentService appointmentService;

    @Test
    void recordVisit_asPatient_isForbidden() throws Exception {
        long appointmentId = attendedAppointment("visitapi-patient");
        MockHttpSession patientSession = login("visitapi-viewer", "secret", Role.PATIENT);

        mockMvc.perform(post("/api/appointments/" + appointmentId + "/visit-record")
                        .session(patientSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnosis\":\"Sprained ankle\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void recordVisit_asAdmin_forAttendedAppointment_returns201WithRecord() throws Exception {
        long appointmentId = attendedAppointment("visitapi-ok");
        MockHttpSession admin = login("visitapi-desk1", "secret", Role.ADMIN);

        mockMvc.perform(post("/api/appointments/" + appointmentId + "/visit-record")
                        .session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnosis\":\"Sprained ankle\",\"prescription\":\"Ibuprofen 400mg\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recorded").value(true))
                .andExpect(jsonPath("$.record.diagnosis").value("Sprained ankle"))
                .andExpect(jsonPath("$.record.prescription").value("Ibuprofen 400mg"));

        mockMvc.perform(get("/api/appointments/" + appointmentId + "/visit-record").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosis").value("Sprained ankle"));
    }

    @Test
    void recordVisit_asAdmin_forNonAttendedAppointment_returns200WithReason() throws Exception {
        // Confirmed, not yet attended - a policy rejection, surfaced as 200 recorded=false.
        Patient patient = patientRepository.save(
                new Patient("Tigist Alemu", LocalDate.of(1990, 1, 1), "visitapi-conf", "secret", "0911111111"));
        Slot slot = slotRepository.save(new Slot(FIXED_NOW.plusHours(5)));
        BookingOutcome booking = appointmentService.requestBooking(patient.getId(), slot.getId());
        appointmentService.confirm(booking.appointment().getId());
        MockHttpSession admin = login("visitapi-desk2", "secret", Role.ADMIN);

        mockMvc.perform(post("/api/appointments/" + booking.appointment().getId() + "/visit-record")
                        .session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnosis\":\"Sprained ankle\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recorded").value(false))
                .andExpect(jsonPath("$.reason").value("APPOINTMENT_NOT_ATTENDED"));
    }

    private long attendedAppointment(String patientUsername) {
        Patient patient = patientRepository.save(
                new Patient("Tigist Alemu", LocalDate.of(1990, 1, 1), patientUsername, "secret", "0911111111"));
        Slot slot = slotRepository.save(new Slot(FIXED_NOW.plusHours(5)));
        BookingOutcome booking = appointmentService.requestBooking(patient.getId(), slot.getId());
        long appointmentId = booking.appointment().getId();
        appointmentService.confirm(appointmentId);
        appointmentService.markAttended(appointmentId);
        return appointmentId;
    }

    private MockHttpSession login(String username, String password, Role role) throws Exception {
        patientRepository.save(
                new Patient(username, LocalDate.of(1990, 1, 1), username, password, "0911000000", role));
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/login").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk());
        return session;
    }
}
