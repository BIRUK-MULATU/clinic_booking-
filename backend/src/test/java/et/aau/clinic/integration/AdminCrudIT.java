package et.aau.clinic.integration;

import et.aau.clinic.domain.Department;
import et.aau.clinic.domain.Doctor;
import et.aau.clinic.domain.Patient;
import et.aau.clinic.domain.Role;
import et.aau.clinic.domain.Slot;
import et.aau.clinic.repository.DepartmentRepository;
import et.aau.clinic.repository.DoctorRepository;
import et.aau.clinic.repository.PatientRepository;
import et.aau.clinic.repository.SlotRepository;
import et.aau.clinic.service.AppointmentService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Hospital-expansion: the update/delete half of the admin's CRUD over
 * doctors, departments, patients, slots and availability. MockMvc
 * against the real controllers, DirectoryService and H2. The through-
 * line is that deletes are guarded: a row a patient's history depends on
 * is kept and the caller told why (400), never silently cascaded.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminCrudIT {

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 1, 10, 9, 0);

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
    private DoctorRepository doctorRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private SlotRepository slotRepository;
    @Autowired
    private AppointmentService appointmentService;

    // --- doctors ---

    @Test
    void updateDoctor_changesNameAndSpecialty() throws Exception {
        Doctor doctor = seedDoctor("Dr. Old", "GP");
        MockHttpSession admin = login("crud-admin1", Role.ADMIN);

        mockMvc.perform(put("/api/doctors/" + doctor.getId()).session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dr. New\",\"specialty\":\"Cardiology\",\"departmentId\":"
                                + doctor.getDepartment().getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Dr. New"))
                .andExpect(jsonPath("$.specialty").value("Cardiology"));
    }

    @Test
    void deleteDoctor_withNoAppointments_removesTheDoctorAndTheirSlots() throws Exception {
        Doctor doctor = seedDoctor("Dr. Spare", "GP");
        MockHttpSession admin = login("crud-admin2", Role.ADMIN);
        slotRepository.save(new Slot(FIXED_NOW.plusDays(2), doctor));

        mockMvc.perform(delete("/api/doctors/" + doctor.getId()).session(admin))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/doctors").session(admin))
                .andExpect(jsonPath("$[?(@.name=='Dr. Spare')]", hasSize(0)));
        org.assertj.core.api.Assertions.assertThat(slotRepository.findByDoctor(doctor)).isEmpty();
    }

    @Test
    void deleteDoctor_withAnAppointment_isRefused() throws Exception {
        Doctor doctor = seedDoctor("Dr. Busy", "GP");
        Slot slot = slotRepository.save(new Slot(FIXED_NOW.plusDays(2), doctor));
        Patient patient = patientRepository.save(
                new Patient("P", LocalDate.of(1990, 1, 1), "crud-p1", "secret", "0911000000"));
        appointmentService.requestBooking(patient.getId(), slot.getId());
        MockHttpSession admin = login("crud-admin3", Role.ADMIN);

        mockMvc.perform(delete("/api/doctors/" + doctor.getId()).session(admin))
                .andExpect(status().isBadRequest());
    }

    // --- departments ---

    @Test
    void renameDepartment_thenDeleteWhenEmpty() throws Exception {
        Department department = departmentRepository.save(new Department("Old Name"));
        MockHttpSession admin = login("crud-admin4", Role.ADMIN);

        mockMvc.perform(put("/api/departments/" + department.getId()).session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"New Name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"));

        mockMvc.perform(delete("/api/departments/" + department.getId()).session(admin))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteDepartment_withDoctors_isRefused() throws Exception {
        Doctor doctor = seedDoctor("Dr. In Dept", "GP");
        MockHttpSession admin = login("crud-admin5", Role.ADMIN);

        mockMvc.perform(delete("/api/departments/" + doctor.getDepartment().getId()).session(admin))
                .andExpect(status().isBadRequest());
    }

    // --- patients ---

    @Test
    void updatePatient_changesDetailsAndResetsPassword() throws Exception {
        Patient patient = patientRepository.save(
                new Patient("Old Name", LocalDate.of(1990, 1, 1), "crud-p2", "oldpass", "0911000000"));
        MockHttpSession admin = login("crud-admin6", Role.ADMIN);

        mockMvc.perform(put("/api/admin/patients/" + patient.getId()).session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Name\",\"dateOfBirth\":\"1985-06-15\",\"phone\":\"0922\","
                                + "\"password\":\"newpass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.dateOfBirth").value("1985-06-15"));

        MockHttpSession asPatient = new MockHttpSession();
        mockMvc.perform(post("/api/login").session(asPatient)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"crud-p2\",\"password\":\"newpass\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void deletePatient_withNoAppointments_removesThem_withAppointmentsIsRefused() throws Exception {
        MockHttpSession admin = login("crud-admin7", Role.ADMIN);
        Patient free = patientRepository.save(
                new Patient("Free", LocalDate.of(1990, 1, 1), "crud-free", "secret", "0911000000"));
        mockMvc.perform(delete("/api/admin/patients/" + free.getId()).session(admin))
                .andExpect(status().isNoContent());

        Patient booked = patientRepository.save(
                new Patient("Booked", LocalDate.of(1990, 1, 1), "crud-booked", "secret", "0911000000"));
        Slot slot = slotRepository.save(new Slot(FIXED_NOW.plusDays(2)));
        appointmentService.requestBooking(booked.getId(), slot.getId());
        mockMvc.perform(delete("/api/admin/patients/" + booked.getId()).session(admin))
                .andExpect(status().isBadRequest());
    }

    // --- slots ---

    @Test
    void deleteSlot_freeSlotGoes_bookedSlotIsRefused() throws Exception {
        MockHttpSession admin = login("crud-admin8", Role.ADMIN);
        Slot free = slotRepository.save(new Slot(FIXED_NOW.plusDays(2)));
        mockMvc.perform(delete("/api/slots/" + free.getId()).session(admin))
                .andExpect(status().isNoContent());

        Slot booked = slotRepository.save(new Slot(FIXED_NOW.plusDays(3)));
        Patient patient = patientRepository.save(
                new Patient("P", LocalDate.of(1990, 1, 1), "crud-p3", "secret", "0911000000"));
        appointmentService.requestBooking(patient.getId(), booked.getId());
        mockMvc.perform(delete("/api/slots/" + booked.getId()).session(admin))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSlot_movesAFreeSlot() throws Exception {
        MockHttpSession admin = login("crud-admin9", Role.ADMIN);
        Slot slot = slotRepository.save(new Slot(FIXED_NOW.plusDays(2)));

        mockMvc.perform(put("/api/slots/" + slot.getId()).session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startTime\":\"" + FIXED_NOW.plusDays(5).withHour(14).withMinute(0) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startTime").value(
                        FIXED_NOW.plusDays(5).withHour(14).withMinute(0).toString()));
    }

    // --- availability ---

    @Test
    void deleteAvailabilityRule_removesItAndRegeneratesSlots() throws Exception {
        Doctor doctor = seedDoctor("Dr. Sched", "GP");
        MockHttpSession admin = login("crud-admin10", Role.ADMIN);

        String created = mockMvc.perform(post("/api/doctors/" + doctor.getId() + "/availability").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dayOfWeek\":\"MONDAY\",\"startTime\":\"09:00\",\"endTime\":\"12:00\","
                                + "\"slotDurationMinutes\":30}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long ruleId = com.jayway.jsonpath.JsonPath.parse(created).read("$.id", Integer.class);

        mockMvc.perform(delete("/api/doctors/" + doctor.getId() + "/availability/" + ruleId).session(admin))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/doctors/" + doctor.getId() + "/availability").session(admin))
                .andExpect(jsonPath("$", hasSize(0)));
        org.assertj.core.api.Assertions.assertThat(slotRepository.findByDoctor(doctor)).isEmpty();
    }

    // --- role guard ---

    @Test
    void crudDeletes_asPatient_areForbidden() throws Exception {
        MockHttpSession patient = login("crud-patient", Role.PATIENT);
        mockMvc.perform(delete("/api/doctors/1").session(patient)).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/departments/1").session(patient)).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/admin/patients/1").session(patient)).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/slots/1").session(patient)).andExpect(status().isForbidden());
    }

    private Doctor seedDoctor(String name, String specialty) {
        Department department = departmentRepository.save(new Department("Dept " + name));
        return doctorRepository.save(new Doctor(name, specialty, department));
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
