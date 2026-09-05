package et.aau.clinic.integration;

import et.aau.clinic.domain.Department;
import et.aau.clinic.domain.Doctor;
import et.aau.clinic.domain.Patient;
import et.aau.clinic.domain.Role;
import et.aau.clinic.repository.DepartmentRepository;
import et.aau.clinic.repository.DoctorRepository;
import et.aau.clinic.repository.PatientRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Hospital-expansion: the reception-only management endpoints added so
 * the admin runs the clinic - creating patient accounts, adding slots,
 * booking on a patient's behalf, and putting a photo on a doctor.
 * MockMvc against real controllers, session, services and H2, with a
 * real session logged in via /api/login.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminManagementIT {

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 1, 10, 9, 0);
    // A tiny valid 1x1 gif as a data URL, so the photo endpoints have something real to store.
    private static final String TINY_IMAGE =
            "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";

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
    private DoctorRepository doctorRepository;
    @Autowired
    private DepartmentRepository departmentRepository;

    // --- admin-created patient accounts ---

    @Test
    void createPatient_asPatient_isForbidden() throws Exception {
        MockHttpSession patient = login("mgmt-patient", Role.PATIENT);
        mockMvc.perform(post("/api/admin/patients").session(patient)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newPatientJson("New Person", "1995-04-04", "newp", "secret")))
                .andExpect(status().isForbidden());
    }

    @Test
    void createPatient_asAdmin_thenThatPatientCanLogIn() throws Exception {
        MockHttpSession admin = login("mgmt-admin1", Role.ADMIN);

        mockMvc.perform(post("/api/admin/patients").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newPatientJson("Meron Haile", "1995-04-04", "meron", "secret")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created").value(true))
                .andExpect(jsonPath("$.patient.username").value("meron"))
                .andExpect(jsonPath("$.patient.password").doesNotExist());

        MockHttpSession newPatient = new MockHttpSession();
        mockMvc.perform(post("/api/login").session(newPatient)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"meron\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PATIENT"));
    }

    @Test
    void createPatient_duplicateUsername_returnsCreatedFalseWithReason() throws Exception {
        patientRepository.save(new Patient("Existing", LocalDate.of(1990, 1, 1), "taken", "secret", "0911000000"));
        MockHttpSession admin = login("mgmt-admin2", Role.ADMIN);

        mockMvc.perform(post("/api/admin/patients").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newPatientJson("Someone Else", "1990-01-01", "taken", "secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(false))
                .andExpect(jsonPath("$.reason").value("USERNAME_TAKEN"));
    }

    @Test
    void createPatient_shortPassword_returnsCreatedFalseWithReason() throws Exception {
        MockHttpSession admin = login("mgmt-admin3", Role.ADMIN);
        mockMvc.perform(post("/api/admin/patients").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newPatientJson("Short Pass", "1990-01-01", "shorty", "abc")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value("PASSWORD_TOO_SHORT"));
    }

    // --- admin-created slots ---

    @Test
    void addSlot_asAdmin_showsUpInTheUpcomingSlotList() throws Exception {
        Doctor doctor = seedDoctor();
        MockHttpSession admin = login("mgmt-admin4", Role.ADMIN);

        mockMvc.perform(post("/api/slots").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doctorId\":" + doctor.getId() + ",\"startTime\":\"" + FIXED_NOW.plusDays(2) + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.doctor.name").value("Dr. Test"));

        mockMvc.perform(get("/api/admin/slots").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void addSlot_inThePast_isRejected() throws Exception {
        MockHttpSession admin = login("mgmt-admin5", Role.ADMIN);
        mockMvc.perform(post("/api/slots").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startTime\":\"" + FIXED_NOW.minusHours(1) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addSlot_asPatient_isForbidden() throws Exception {
        MockHttpSession patient = login("mgmt-patient2", Role.PATIENT);
        mockMvc.perform(post("/api/slots").session(patient)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startTime\":\"" + FIXED_NOW.plusDays(1) + "\"}"))
                .andExpect(status().isForbidden());
    }

    // --- admin books for a patient ---

    @Test
    void bookForPatient_asAdmin_createsConfirmedAppointment() throws Exception {
        Doctor doctor = seedDoctor();
        Patient patient = patientRepository.save(
                new Patient("Booked Person", LocalDate.of(1990, 1, 1), "booked", "secret", "0911000000"));
        MockHttpSession admin = login("mgmt-admin6", Role.ADMIN);

        String slotResponse = mockMvc.perform(post("/api/slots").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doctorId\":" + doctor.getId() + ",\"startTime\":\"" + FIXED_NOW.plusDays(1) + "\"}"))
                .andReturn().getResponse().getContentAsString();
        long slotId = com.jayway.jsonpath.JsonPath.parse(slotResponse).read("$.id", Integer.class);

        mockMvc.perform(post("/api/admin/appointments").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":" + patient.getId() + ",\"slotId\":" + slotId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.approved").value(true))
                .andExpect(jsonPath("$.appointment.status").value("CONFIRMED"));
    }

    @Test
    void bookForPatient_asPatient_isForbidden() throws Exception {
        MockHttpSession patient = login("mgmt-patient3", Role.PATIENT);
        mockMvc.perform(post("/api/admin/appointments").session(patient)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":1,\"slotId\":1}"))
                .andExpect(status().isForbidden());
    }

    // --- doctor photo + availability ---

    @Test
    void setDoctorPhoto_asAdmin_thenPhotoAndAvailabilityAppearInTheList() throws Exception {
        Doctor doctor = seedDoctor();
        MockHttpSession admin = login("mgmt-admin7", Role.ADMIN);

        mockMvc.perform(post("/api/doctors/" + doctor.getId() + "/availability").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dayOfWeek\":\"MONDAY\",\"startTime\":\"09:00\",\"endTime\":\"12:00\","
                                + "\"slotDurationMinutes\":30}"))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/doctors/" + doctor.getId() + "/photo").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"photo\":\"" + TINY_IMAGE + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/doctors").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].photo").value(TINY_IMAGE))
                .andExpect(jsonPath("$[0].availability", hasSize(1)))
                .andExpect(jsonPath("$[0].availability[0].dayOfWeek").value("MONDAY"));
    }

    @Test
    void setDoctorPhoto_nonImageData_isRejected() throws Exception {
        Doctor doctor = seedDoctor();
        MockHttpSession admin = login("mgmt-admin8", Role.ADMIN);
        mockMvc.perform(put("/api/doctors/" + doctor.getId() + "/photo").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"photo\":\"data:text/plain;base64,aGVsbG8=\"}"))
                .andExpect(status().isBadRequest());
    }

    private Doctor seedDoctor() {
        Department department = departmentRepository.save(new Department("Test Dept"));
        return doctorRepository.save(new Doctor("Dr. Test", "Tester", department));
    }

    private String newPatientJson(String name, String dob, String username, String password) {
        return "{\"name\":\"" + name + "\",\"dateOfBirth\":\"" + dob + "\",\"phone\":\"0911000000\","
                + "\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
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
