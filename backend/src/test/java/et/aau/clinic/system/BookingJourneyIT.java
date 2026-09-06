package et.aau.clinic.system;

import et.aau.clinic.domain.Appointment;
import et.aau.clinic.domain.AppointmentStatus;
import et.aau.clinic.domain.FeeCategory;
import et.aau.clinic.domain.Patient;
import et.aau.clinic.domain.Slot;
import et.aau.clinic.repository.AppointmentRepository;
import et.aau.clinic.repository.PatientRepository;
import et.aau.clinic.repository.SlotRepository;
import et.aau.clinic.system.pages.BookingPage;
import et.aau.clinic.system.pages.ConfirmationPage;
import et.aau.clinic.system.pages.LoginPage;
import et.aau.clinic.system.pages.MyAppointmentsPage;
import et.aau.clinic.system.pages.SlotsPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 7: the one system-level test, driving a real Chrome browser
 * against the real embedded server via the Page Object pattern. Per
 * CLAUDE.md this covers exactly one journey: log in, view slots,
 * book, see the fee on the confirmation page, cancel.
 *
 * No @Transactional here - Selenium talks to the app over a real HTTP
 * socket from a different thread than the test method, so an open,
 * uncommitted test transaction would simply be invisible to it. Test
 * data is seeded with plain repository saves (auto-committed) using
 * unique usernames per test instead.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookingJourneyIT {

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 1, 10, 10, 0);

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(FIXED_NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private SlotRepository slotRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;

    private String baseUrl;
    private WebDriver driver;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu", "--window-size=1920,1080",
                "--no-sandbox", "--disable-dev-shm-usage");
        driver = new ChromeDriver(options);
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void login_wrongPassword_showsErrorOnLoginPage() {
        patientRepository.save(new Patient(
                "Selam Wolde", LocalDate.of(1992, 4, 4), "seleniumBadLogin", "secret", "0911222001"));

        LoginPage loginPage = new LoginPage(driver, baseUrl).open();
        loginPage.submitInvalidLogin("seleniumBadLogin", "wrong-password");

        assertThat(loginPage.getErrorMessage()).isEqualTo("Invalid username or password.");
    }

    @Test
    void fullJourney_loginViewSlotsBookSeeFeeOnConfirmationThenCancel() {
        patientRepository.save(new Patient(
                "Selam Wolde", LocalDate.of(1992, 4, 4), "seleniumJourney", "secret", "0911222002"));
        Slot slot = slotRepository.save(new Slot(FIXED_NOW.plusHours(5)));

        SlotsPage slotsPage = new LoginPage(driver, baseUrl).open().loginAs("seleniumJourney", "secret");

        ConfirmationPage confirmationPage = slotsPage.bookSlot(slot.getId()).confirmBooking();

        assertThat(confirmationPage.getStatus()).isEqualTo("REQUESTED");
        assertThat(confirmationPage.getFeeCategory()).isEqualTo("ADULT");
        assertThat(confirmationPage.getFeeAmount()).isEqualTo("250.00");

        Long appointmentId = confirmationPage.getAppointmentId();
        MyAppointmentsPage myAppointmentsPage = confirmationPage.goToMyAppointments();
        assertThat(myAppointmentsPage.getStatus(appointmentId)).isEqualTo("REQUESTED");

        myAppointmentsPage.cancel(appointmentId);

        assertThat(myAppointmentsPage.getStatus(appointmentId)).isEqualTo("CANCELLED");
    }

    // Rule G - the insured patient sees their reduced "you pay" amount on the confirmation page.
    @Test
    void insuredPatient_seesNetPayableOnConfirmation() {
        Patient insured = new Patient(
                "Meron Bekele", LocalDate.of(1985, 6, 6), "seleniumInsured", "secret", "0911222003");
        insured.setCoveragePercent(40);
        patientRepository.save(insured);
        Slot slot = slotRepository.save(new Slot(FIXED_NOW.plusHours(5)));

        ConfirmationPage confirmation = new LoginPage(driver, baseUrl).open()
                .loginAs("seleniumInsured", "secret")
                .bookSlot(slot.getId())
                .confirmBooking();

        assertThat(confirmation.getFeeAmount()).isEqualTo("250.00");
        assertThat(confirmation.getNetPayable()).isEqualTo("150.00"); // 250 - 40%
    }

    // Rule H - a patient with 3 no-shows in the last 90 days is blocked from booking online.
    @Test
    void patientSuspendedForNoShows_isBlockedOnTheBookingPage() {
        Patient suspended = patientRepository.save(new Patient(
                "Dawit Alemu", LocalDate.of(1990, 3, 3), "seleniumSuspended", "secret", "0911222004"));
        for (int i = 1; i <= 3; i++) {
            Slot missed = slotRepository.save(new Slot(FIXED_NOW.minusDays(i)));
            appointmentRepository.save(new Appointment(suspended, missed, AppointmentStatus.NO_SHOW,
                    FeeCategory.ADULT, new BigDecimal("250"), FIXED_NOW.minusDays(i + 1)));
        }
        Slot slot = slotRepository.save(new Slot(FIXED_NOW.plusHours(5)));

        BookingPage bookingPage = new LoginPage(driver, baseUrl).open()
                .loginAs("seleniumSuspended", "secret")
                .bookSlot(slot.getId())
                .submitExpectingRejection();

        assertThat(bookingPage.getErrorMessage()).isEqualTo("SUSPENDED_NO_SHOWS");
    }

    // Rule I - the patient moves an appointment to a different slot from the my-appointments page.
    @Test
    void patientReschedulesAnAppointmentToADifferentSlot() {
        patientRepository.save(new Patient(
                "Sara Nega", LocalDate.of(1993, 7, 7), "seleniumReschedule", "secret", "0911222005"));
        Slot first = slotRepository.save(new Slot(FIXED_NOW.plusHours(5)));
        Slot second = slotRepository.save(new Slot(FIXED_NOW.plusDays(1).withHour(9).withMinute(0)));

        ConfirmationPage confirmation = new LoginPage(driver, baseUrl).open()
                .loginAs("seleniumReschedule", "secret")
                .bookSlot(first.getId())
                .confirmBooking();
        Long appointmentId = confirmation.getAppointmentId();

        MyAppointmentsPage myAppointments = confirmation.goToMyAppointments();
        assertThat(myAppointments.getSlotTime(appointmentId)).startsWith("2026-01-10");

        myAppointments.reschedule(appointmentId, second.getId());

        assertThat(myAppointments.getStatus(appointmentId)).isEqualTo("REQUESTED");
        assertThat(myAppointments.getSlotTime(appointmentId)).isEqualTo("2026-01-11 09:00");
    }
}
