package et.aau.clinic.integration;

import et.aau.clinic.domain.Appointment;
import et.aau.clinic.domain.AppointmentStatus;
import et.aau.clinic.domain.Patient;
import et.aau.clinic.domain.RejectionReason;
import et.aau.clinic.domain.Slot;
import et.aau.clinic.repository.AppointmentRepository;
import et.aau.clinic.repository.PatientRepository;
import et.aau.clinic.repository.SlotRepository;
import et.aau.clinic.service.AppointmentService;
import et.aau.clinic.service.BookingOutcome;
import et.aau.clinic.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Phase 6: AppointmentService wired to real Spring Data JPA repositories
 * and a real H2 database (our "fake" - CLAUDE.md) instead of Mockito
 * mocks. This is what proves the repository queries behind C1
 * (existsBySlotAndStatusIn) actually work, not just the boolean logic
 * around them.
 *
 * notificationService is a Mockito @SpyBean wrapping the real
 * LoggingNotificationService - it still logs for real, but calls can
 * be verified. That is the fourth test double type CLAUDE.md asks for
 * (stub: Clock: mock: Phase 4 unit tests: fake: H2: spy: here).
 *
 * @Transactional so each test's writes roll back afterwards - the
 * Spring context (and its H2 database) is reused across all test
 * methods in this class, and unique constraints like Patient.username
 * would otherwise collide between tests.
 */
@SpringBootTest
@Transactional
class AppointmentServiceIT {

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
    private AppointmentService appointmentService;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private SlotRepository slotRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;
    @SpyBean
    private NotificationService notificationService;

    private Patient patient;

    @BeforeEach
    void setUp() {
        patient = patientRepository.save(
                new Patient("Tigist Alemu", LocalDate.of(1990, 1, 1), "tigist", "secret", "0911111111"));
    }

    @Test
    void requestBooking_persistsAppointmentReadableFromRepository() {
        Slot slot = slotRepository.save(new Slot(FIXED_NOW.plusHours(5)));

        BookingOutcome outcome = appointmentService.requestBooking(patient.getId(), slot.getId());

        assertThat(outcome.decision().isApproved()).isTrue();
        Appointment persisted = appointmentRepository.findById(outcome.appointment().getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(AppointmentStatus.REQUESTED);
        assertThat(persisted.getFeeAmount()).isEqualByComparingTo(new BigDecimal("250"));
    }

    @Test
    void requestBooking_slotAlreadyTakenByAnotherPatient_rejectedBasedOnRealRepositoryQuery() {
        Slot slot = slotRepository.save(new Slot(FIXED_NOW.plusHours(5)));
        appointmentService.requestBooking(patient.getId(), slot.getId());

        Patient other = patientRepository.save(
                new Patient("Dawit Assefa", LocalDate.of(1985, 3, 3), "dawit", "secret", "0911111112"));

        BookingOutcome outcome = appointmentService.requestBooking(other.getId(), slot.getId());

        assertThat(outcome.decision().isApproved()).isFalse();
        assertThat(outcome.decision().getReason()).isEqualTo(RejectionReason.SLOT_UNAVAILABLE);
    }

    @Test
    void confirm_realNotificationServiceSpyReceivesTheCall() {
        Slot slot = slotRepository.save(new Slot(FIXED_NOW.plusHours(5)));
        BookingOutcome outcome = appointmentService.requestBooking(patient.getId(), slot.getId());

        appointmentService.confirm(outcome.appointment().getId());

        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(notificationService).sendConfirmation(patientCaptor.capture(), any(Appointment.class));
        assertThat(patientCaptor.getValue().getId()).isEqualTo(patient.getId());
    }

    @Test
    void cancel_confirmedAppointmentWithinLateWindow_persistsHalfFee() {
        Slot slot = slotRepository.save(new Slot(FIXED_NOW.plusHours(10)));
        BookingOutcome outcome = appointmentService.requestBooking(patient.getId(), slot.getId());
        Long appointmentId = outcome.appointment().getId();
        appointmentService.confirm(appointmentId);

        appointmentService.cancel(appointmentId);

        Appointment persisted = appointmentRepository.findById(appointmentId).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(persisted.getCancellationFee()).isEqualByComparingTo(new BigDecimal("125.0"));
    }

    @Test
    void requestBooking_afterThreeNoShows_isRejectedForSelfBooking_butReceptionCanStillBook() {
        // Rack up 3 no-shows through the real flow: book -> confirm -> markNoShow, on 3 slots.
        for (int i = 1; i <= 3; i++) {
            Slot missed = slotRepository.save(new Slot(FIXED_NOW.plusHours(2 + i)));
            BookingOutcome b = appointmentService.requestBooking(patient.getId(), missed.getId());
            appointmentService.confirm(b.appointment().getId());
            appointmentService.markNoShow(b.appointment().getId());
        }

        Slot fresh = slotRepository.save(new Slot(FIXED_NOW.plusDays(10)));
        BookingOutcome selfBooking = appointmentService.requestBooking(patient.getId(), fresh.getId());
        assertThat(selfBooking.decision().isApproved()).isFalse();
        assertThat(selfBooking.decision().getReason()).isEqualTo(RejectionReason.SUSPENDED_NO_SHOWS);

        BookingOutcome receptionBooking = appointmentService.bookForPatient(patient.getId(), fresh.getId());
        assertThat(receptionBooking.decision().isApproved()).isTrue();
    }

    @Test
    void requestBooking_insuredPatient_persistsNetPayableAlongsideTheFullFee() {
        patient.setCoveragePercent(60);
        patientRepository.save(patient);
        Slot slot = slotRepository.save(new Slot(FIXED_NOW.plusHours(5)));

        BookingOutcome outcome = appointmentService.requestBooking(patient.getId(), slot.getId());

        Appointment persisted = appointmentRepository.findById(outcome.appointment().getId()).orElseThrow();
        assertThat(persisted.getFeeAmount()).isEqualByComparingTo("250");     // Rule 1, unchanged
        assertThat(persisted.getNetPayable()).isEqualByComparingTo("100.00"); // Rule G: 250 - 60%
    }

    @Test
    void sendDueReminders_confirmedAppointmentWithin24h_remindsOnceAndNotAgainOnTheNextSweep() {
        Slot slot = slotRepository.save(new Slot(FIXED_NOW.plusHours(5)));
        BookingOutcome outcome = appointmentService.requestBooking(patient.getId(), slot.getId());
        Long appointmentId = outcome.appointment().getId();
        appointmentService.confirm(appointmentId);

        int firstSweep = appointmentService.sendDueReminders();
        int secondSweep = appointmentService.sendDueReminders();

        assertThat(firstSweep).isEqualTo(1);
        assertThat(secondSweep).isZero(); // reminderSentAt is now set, so the row is filtered out
        verify(notificationService).sendReminder(any(Patient.class), any(Appointment.class));
        Appointment persisted = appointmentRepository.findById(appointmentId).orElseThrow();
        assertThat(persisted.getReminderSentAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    void sendDueReminders_confirmedAppointmentStillDaysAway_sendsNothing() {
        Slot slot = slotRepository.save(new Slot(FIXED_NOW.plusDays(4)));
        BookingOutcome outcome = appointmentService.requestBooking(patient.getId(), slot.getId());
        appointmentService.confirm(outcome.appointment().getId());

        assertThat(appointmentService.sendDueReminders()).isZero();
        verify(notificationService, never()).sendReminder(any(), any());
    }

    @Test
    void reschedule_movesTheAppointmentToTheNewSlotAndFreesTheOldOne() {
        Slot oldSlot = slotRepository.save(new Slot(FIXED_NOW.plusHours(5)));
        Slot newSlot = slotRepository.save(new Slot(FIXED_NOW.plusDays(2)));
        BookingOutcome outcome = appointmentService.requestBooking(patient.getId(), oldSlot.getId());
        Long appointmentId = outcome.appointment().getId();
        appointmentService.confirm(appointmentId);

        var rescheduled = appointmentService.reschedule(appointmentId, newSlot.getId());

        assertThat(rescheduled.decision().isApproved()).isTrue();
        Appointment persisted = appointmentRepository.findById(appointmentId).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(persisted.getSlot().getId()).isEqualTo(newSlot.getId());
        assertThat(appointmentService.listAvailableSlots()).extracting(Slot::getId).contains(oldSlot.getId());
    }

    @Test
    void expireStaleWaitlistOffers_lapsesAnUnconfirmedPromotionToOfferExpired() {
        Slot slot = slotRepository.save(new Slot(FIXED_NOW.plusDays(2)));
        Patient second = patientRepository.save(
                new Patient("Second In Line", LocalDate.of(1991, 2, 2), "second", "secret", "0911111133"));

        Long confirmedId = appointmentService.requestBooking(patient.getId(), slot.getId()).appointment().getId();
        appointmentService.confirm(confirmedId);
        Long secondId = appointmentService.joinWaitlist(second.getId(), slot.getId()).getId();

        // Holder cancels -> the waitlisted patient is promoted to REQUESTED with an offer timestamp.
        appointmentService.cancel(confirmedId);
        Appointment promoted = appointmentRepository.findById(secondId).orElseThrow();
        assertThat(promoted.getStatus()).isEqualTo(AppointmentStatus.REQUESTED);
        assertThat(promoted.getWaitlistOfferedAt()).isEqualTo(FIXED_NOW);
        // Simulate the 2-hour acceptance window elapsing by backdating the offer.
        promoted.setWaitlistOfferedAt(FIXED_NOW.minusHours(3));
        appointmentRepository.save(promoted);

        int expired = appointmentService.expireStaleWaitlistOffers();

        assertThat(expired).isEqualTo(1);
        assertThat(appointmentRepository.findById(secondId).orElseThrow().getStatus())
                .isEqualTo(AppointmentStatus.OFFER_EXPIRED);
    }

    @Test
    void listAvailableSlots_excludesSlotJustBookedThroughTheRealRepository() {
        Slot slot = slotRepository.save(new Slot(FIXED_NOW.plusHours(5)));
        appointmentService.requestBooking(patient.getId(), slot.getId());

        assertThat(appointmentService.listAvailableSlots())
                .extracting(Slot::getId)
                .doesNotContain(slot.getId());
    }
}
