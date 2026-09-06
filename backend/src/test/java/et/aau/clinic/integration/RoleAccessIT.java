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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Hospital-expansion roles/admin layer: the PATIENT vs ADMIN split added
 * for the decorated React frontend. These endpoints have no other
 * automated coverage, so this class is the evidence that the guard
 * actually holds - a 401 with no session, a 403 for a PATIENT, and a
 * 200 for an ADMIN, across the reception-only endpoints
 * (DoctorApiController, QueueApiController, AdminApiController,
 * AppointmentApiController.confirm). Plus one behavioural check that the
 * day roster comes back in slot order.
 *
 * Driven through MockMvc with a real session logged in via /api/login,
 * so the session attribute the guard reads ("role") is set exactly the
 * way a real browser would set it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RoleAccessIT {

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 1, 10, 8, 0);

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
    void receptionEndpoints_withNoSession_return401() throws Exception {
        mockMvc.perform(get("/api/queue")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/doctors")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/appointments")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/appointments/pending")).andExpect(status().isUnauthorized());
    }

    @Test
    void receptionEndpoints_asPatient_return403() throws Exception {
        MockHttpSession patient = login("role-patient", Role.PATIENT);

        mockMvc.perform(get("/api/queue").session(patient)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/doctors").session(patient)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/departments").session(patient)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/appointments").session(patient)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/appointments/pending").session(patient)).andExpect(status().isForbidden());
    }

    @Test
    void receptionEndpoints_asAdmin_return200() throws Exception {
        MockHttpSession admin = login("role-admin", Role.ADMIN);

        mockMvc.perform(get("/api/queue").session(admin)).andExpect(status().isOk());
        mockMvc.perform(get("/api/doctors").session(admin)).andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/appointments").session(admin)).andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/appointments/pending").session(admin)).andExpect(status().isOk());
    }

    @Test
    void pendingAppointments_listsEveryRequestedAppointment_regardlessOfDate() throws Exception {
        // A REQUESTED appointment for a future date is invisible on the date roster (which
        // defaults to today) but must always show on the confirm queue - the bug this endpoint fixes.
        Patient patient = patientRepository.save(
                new Patient("Tigist Alemu", LocalDate.of(1990, 1, 1), "role-pending", "secret", "0911111111"));
        Slot nextWeek = slotRepository.save(new Slot(FIXED_NOW.plusDays(7)));
        appointmentService.requestBooking(patient.getId(), nextWeek.getId());

        MockHttpSession admin = login("role-admin-pending", Role.ADMIN);

        mockMvc.perform(get("/api/admin/appointments").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/admin/appointments/pending").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].patientName").value("Tigist Alemu"))
                .andExpect(jsonPath("$[0].status").value("REQUESTED"));
    }

    @Test
    void confirmAppointment_asPatient_isForbidden_asAdmin_succeeds() throws Exception {
        Patient patient = patientRepository.save(
                new Patient("Tigist Alemu", LocalDate.of(1990, 1, 1), "role-booker", "secret", "0911111111"));
        Slot slot = slotRepository.save(new Slot(FIXED_NOW.plusHours(5)));
        BookingOutcome booking = appointmentService.requestBooking(patient.getId(), slot.getId());
        long appointmentId = booking.appointment().getId();

        MockHttpSession patientSession = login("role-patient2", Role.PATIENT);
        mockMvc.perform(post("/api/appointments/" + appointmentId + "/confirm").session(patientSession))
                .andExpect(status().isForbidden());

        MockHttpSession admin = login("role-admin2", Role.ADMIN);
        mockMvc.perform(post("/api/appointments/" + appointmentId + "/confirm").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void reschedule_asThePatient_movesTheAppointment_orReportsThePolicyReason() throws Exception {
        Patient patient = patientRepository.save(
                new Patient("Tigist Alemu", LocalDate.of(1990, 1, 1), "role-resched-owner", "secret", "0911111111"));
        Slot oldSlot = slotRepository.save(new Slot(FIXED_NOW.plusHours(5)));
        Slot newSlot = slotRepository.save(new Slot(FIXED_NOW.plusDays(2)));
        Slot soonSlot = slotRepository.save(new Slot(FIXED_NOW.plusMinutes(30)));
        long appointmentId = appointmentService.requestBooking(patient.getId(), oldSlot.getId()).appointment().getId();

        MockHttpSession session = login("role-resched", Role.PATIENT);

        // Too little notice on the target slot -> rejected with the ReschedulePolicy reason.
        mockMvc.perform(post("/api/appointments/" + appointmentId + "/reschedule").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slotId\":" + soonSlot.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved").value(false))
                .andExpect(jsonPath("$.reason").value("INSUFFICIENT_NOTICE"));

        // A free slot with plenty of notice -> moved.
        mockMvc.perform(post("/api/appointments/" + appointmentId + "/reschedule").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slotId\":" + newSlot.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved").value(true))
                .andExpect(jsonPath("$.appointment.slotId").value(newSlot.getId().intValue()))
                .andExpect(jsonPath("$.appointment.status").value("REQUESTED"));
    }

    @Test
    void adminAppointments_returnsTheDayRosterInSlotOrder() throws Exception {
        Patient patient = patientRepository.save(
                new Patient("Tigist Alemu", LocalDate.of(1990, 1, 1), "role-roster", "secret", "0911111111"));
        Patient other = patientRepository.save(
                new Patient("Kebede Worku", LocalDate.of(1985, 1, 1), "role-roster2", "secret", "0911111112"));
        LocalDate day = FIXED_NOW.toLocalDate().plusDays(1);
        Slot late = slotRepository.save(new Slot(day.atTime(14, 0)));
        Slot early = slotRepository.save(new Slot(day.atTime(9, 0)));
        appointmentService.requestBooking(patient.getId(), late.getId());
        appointmentService.requestBooking(other.getId(), early.getId());

        MockHttpSession admin = login("role-admin3", Role.ADMIN);
        mockMvc.perform(get("/api/admin/appointments").param("date", day.toString()).session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].slotStartTime").value(day.atTime(9, 0).toString()))
                .andExpect(jsonPath("$[1].slotStartTime").value(day.atTime(14, 0).toString()));
    }

    private MockHttpSession login(String username, Role role) throws Exception {
        patientRepository.save(
                new Patient(username, LocalDate.of(1990, 1, 1), username, "secret", "0911000000", role));
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/login").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"secret\"}"))
                .andExpect(status().isOk());
        return session;
    }
}
