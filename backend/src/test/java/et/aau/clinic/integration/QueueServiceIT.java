package et.aau.clinic.integration;

import et.aau.clinic.domain.Appointment;
import et.aau.clinic.domain.AppointmentStatus;
import et.aau.clinic.domain.Patient;
import et.aau.clinic.domain.QueueEntry;
import et.aau.clinic.domain.QueueEntryStatus;
import et.aau.clinic.domain.Slot;
import et.aau.clinic.repository.AppointmentRepository;
import et.aau.clinic.repository.PatientRepository;
import et.aau.clinic.repository.QueueEntryRepository;
import et.aau.clinic.repository.SlotRepository;
import et.aau.clinic.service.AppointmentService;
import et.aau.clinic.service.BookingOutcome;
import et.aau.clinic.service.QueueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Hospital-expansion Phase D: QueueService against real repositories and
 * a real H2 database, proving the completeConsultation() seam actually
 * behaves atomically - not just that AppointmentService.markAttended()
 * gets called (QueueServiceTest already proves that with a mock).
 *
 * Deliberately NOT @Transactional at the class level, unlike
 * AppointmentServiceIT/AppointmentJourneyIT. Those use it purely for
 * test-to-test row isolation. Here it would actively hide the thing
 * this class exists to prove: whether completeConsultation()'s own
 * @Transactional really rolls back the QueueEntry save when the
 * Appointment-side transition fails. Wrapping the whole test method in
 * an outer transaction would make that rollback invisible to the
 * post-exception re-fetch (same reasoning BookingJourneyIT uses to skip
 * @Transactional, for a different concurrency reason). Each test method
 * uses a unique patient username instead, the same isolation strategy
 * BookingJourneyIT uses.
 */
@SpringBootTest
class QueueServiceIT {

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
    private QueueService queueService;
    @Autowired
    private AppointmentService appointmentService;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private SlotRepository slotRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private QueueEntryRepository queueEntryRepository;

    @Test
    void completeConsultation_confirmedAppointment_movesQueueEntryToDoneAndAppointmentToAttended() {
        Long appointmentId = confirmedAppointment("queuehappy");
        Long entryId = queueService.checkIn(appointmentId).getId();
        queueService.call(entryId);
        queueService.startConsultation(entryId);

        queueService.completeConsultation(entryId);

        QueueEntry persistedEntry = queueEntryRepository.findById(entryId).orElseThrow();
        Appointment persistedAppointment = appointmentRepository.findById(appointmentId).orElseThrow();
        assertThat(persistedEntry.getStatus()).isEqualTo(QueueEntryStatus.DONE);
        assertThat(persistedAppointment.getStatus()).isEqualTo(AppointmentStatus.ATTENDED);
    }

    // The seam's failure mode, proven for real: if the Appointment is no longer CONFIRMED by
    // the time completeConsultation() runs (here, cancelled through the normal cancel() path -
    // standing in for a concurrent request), markAttended() throws, and the whole transaction -
    // including the QueueEntry's own save to DONE - must roll back. If it didn't, this would be
    // exactly the permanent, silent mismatch the @Transactional boundary exists to prevent.
    @Test
    void completeConsultation_appointmentNoLongerConfirmed_rollsBackQueueEntryToo() {
        Long appointmentId = confirmedAppointment("queuerollback");
        Long entryId = queueService.checkIn(appointmentId).getId();
        queueService.call(entryId);
        queueService.startConsultation(entryId);

        appointmentService.cancel(appointmentId);

        assertThatThrownBy(() -> queueService.completeConsultation(entryId))
                .isInstanceOf(IllegalStateException.class);

        QueueEntry persistedEntry = queueEntryRepository.findById(entryId).orElseThrow();
        Appointment persistedAppointment = appointmentRepository.findById(appointmentId).orElseThrow();
        assertThat(persistedEntry.getStatus()).isEqualTo(QueueEntryStatus.IN_CONSULTATION);
        assertThat(persistedAppointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    private Long confirmedAppointment(String username) {
        Patient patient = patientRepository.save(
                new Patient("Tigist Alemu", LocalDate.of(1990, 1, 1), username, "secret", "0911111111"));
        Slot slot = slotRepository.save(new Slot(FIXED_NOW.plusHours(5)));
        BookingOutcome outcome = appointmentService.requestBooking(patient.getId(), slot.getId());
        Long appointmentId = outcome.appointment().getId();
        appointmentService.confirm(appointmentId);
        return appointmentId;
    }
}
