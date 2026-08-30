package et.aau.clinic.integration;

import et.aau.clinic.domain.Patient;
import et.aau.clinic.domain.RejectionReason;
import et.aau.clinic.domain.Slot;
import et.aau.clinic.repository.PatientRepository;
import et.aau.clinic.repository.SlotRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Phase 6: the booking journey driven through real HTTP requests
 * (MockMvc) against the real controllers, session, interceptor,
 * Thymeleaf views, service and H2 - everything except an actual
 * browser and TCP socket. Selenium (Phase 7) repeats this same
 * journey through a real browser; this class exists to catch wiring
 * mistakes (view names, redirects, model attribute names) far faster
 * than a browser-driven test could.
 *
 * @Transactional for the same reason as AppointmentServiceIT: one
 * Spring context and H2 database is shared across all tests here.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AppointmentJourneyIT {

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 1, 10, 10, 0);

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

    @Test
    void unauthenticatedRequest_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/slots"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void login_wrongPassword_returnsLoginPageWithError() throws Exception {
        patientRepository.save(
                new Patient("Tigist Alemu", LocalDate.of(1990, 1, 1), "tigist1", "secret", "0911111111"));

        mockMvc.perform(post("/login").param("username", "tigist1").param("password", "wrong"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void fullJourney_loginViewSlotsBookConfirmationCancel() throws Exception {
        patientRepository.save(
                new Patient("Tigist Alemu", LocalDate.of(1990, 1, 1), "tigist2", "secret", "0911111111"));
        Slot slot = slotRepository.save(new Slot(FIXED_NOW.plusHours(5)));

        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/login").session(session)
                        .param("username", "tigist2").param("password", "secret"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/slots"));

        mockMvc.perform(get("/slots").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attribute("slots", hasSize(1)));

        MvcResult bookResult = mockMvc.perform(post("/book").session(session)
                        .param("slotId", slot.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String confirmationUrl = bookResult.getResponse().getRedirectedUrl();

        mockMvc.perform(get(confirmationUrl).session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("confirmation"))
                .andExpect(model().attribute("appointment",
                        hasProperty("feeAmount", comparesEqualTo(new BigDecimal("250")))));

        String appointmentId = confirmationUrl.substring(confirmationUrl.lastIndexOf('/') + 1);

        mockMvc.perform(post("/appointments/" + appointmentId + "/cancel").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my-appointments"));

        mockMvc.perform(get("/my-appointments").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CANCELLED")));
    }

    @Test
    void book_insufficientNotice_showsErrorOnBookingForm() throws Exception {
        patientRepository.save(
                new Patient("Kebede Worku", LocalDate.of(1990, 1, 1), "kebede2", "secret", "0911111113"));
        Slot slot = slotRepository.save(new Slot(FIXED_NOW.plusMinutes(30)));

        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/login").session(session)
                .param("username", "kebede2").param("password", "secret"));

        mockMvc.perform(post("/book").session(session).param("slotId", slot.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("book"))
                .andExpect(model().attribute("error", RejectionReason.INSUFFICIENT_NOTICE));
    }
}
