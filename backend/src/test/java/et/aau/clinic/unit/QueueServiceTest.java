package et.aau.clinic.unit;

import et.aau.clinic.domain.Appointment;
import et.aau.clinic.domain.AppointmentStatus;
import et.aau.clinic.domain.FeeCategory;
import et.aau.clinic.domain.Patient;
import et.aau.clinic.domain.QueueEntry;
import et.aau.clinic.domain.QueueEntryStatus;
import et.aau.clinic.domain.Slot;
import et.aau.clinic.repository.AppointmentRepository;
import et.aau.clinic.repository.QueueEntryRepository;
import et.aau.clinic.service.AppointmentService;
import et.aau.clinic.service.QueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Hospital-expansion Phase D: QueueService against Mockito mocks for
 * both repositories and, importantly, for AppointmentService itself -
 * completeConsultation()'s whole job is to call
 * AppointmentService.markAttended(), so this test verifies that call
 * happens rather than re-testing markAttended()'s own logic (already
 * covered by AppointmentServiceTest).
 */
@ExtendWith(MockitoExtension.class)
class QueueServiceTest {

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 1, 10, 8, 0);
    private static final Clock FIXED_CLOCK =
            Clock.fixed(FIXED_NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Mock
    private QueueEntryRepository queueEntryRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private AppointmentService appointmentService;

    private QueueService service;

    @BeforeEach
    void setUp() {
        service = new QueueService(queueEntryRepository, appointmentRepository, appointmentService, FIXED_CLOCK);
    }

    @Test
    void checkIn_confirmedAppointment_createsWaitingEntryAtClockTime() {
        Appointment appointment = confirmedAppointment();
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(queueEntryRepository.save(any(QueueEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QueueEntry entry = service.checkIn(1L);

        assertThat(entry.getStatus()).isEqualTo(QueueEntryStatus.WAITING);
        assertThat(entry.getCheckedInAt()).isEqualTo(FIXED_NOW);
        assertThat(entry.getAppointment()).isEqualTo(appointment);
    }

    @Test
    void checkIn_appointmentNotConfirmed_throwsAndSavesNothing() {
        Appointment appointment = confirmedAppointment();
        appointment.setStatus(AppointmentStatus.REQUESTED);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.checkIn(1L)).isInstanceOf(IllegalStateException.class);

        verify(queueEntryRepository, never()).save(any());
    }

    @Test
    void call_waitingEntry_movesToCalled() {
        QueueEntry entry = waitingEntry();
        when(queueEntryRepository.findById(5L)).thenReturn(Optional.of(entry));
        when(queueEntryRepository.save(any(QueueEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QueueEntry result = service.call(5L);

        assertThat(result.getStatus()).isEqualTo(QueueEntryStatus.CALLED);
    }

    @Test
    void startConsultation_calledEntry_movesToInConsultation() {
        QueueEntry entry = waitingEntry();
        entry.setStatus(QueueEntryStatus.CALLED);
        when(queueEntryRepository.findById(5L)).thenReturn(Optional.of(entry));
        when(queueEntryRepository.save(any(QueueEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QueueEntry result = service.startConsultation(5L);

        assertThat(result.getStatus()).isEqualTo(QueueEntryStatus.IN_CONSULTATION);
    }

    // The seam itself: completeConsultation() must move the QueueEntry to DONE AND call
    // AppointmentService.markAttended() with the linked appointment's id - proving both
    // sides of the synchronisation happen, not just the queue side.
    @Test
    void completeConsultation_inConsultationEntry_movesToDoneAndDrivesAppointmentToAttended() {
        Appointment appointment = confirmedAppointment();
        QueueEntry entry = waitingEntry();
        entry.setStatus(QueueEntryStatus.IN_CONSULTATION);
        when(queueEntryRepository.findById(5L)).thenReturn(Optional.of(entry));
        when(queueEntryRepository.save(any(QueueEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QueueEntry result = service.completeConsultation(5L);

        assertThat(result.getStatus()).isEqualTo(QueueEntryStatus.DONE);
        verify(appointmentService).markAttended(appointment.getId());
    }

    private Appointment confirmedAppointment() {
        Patient patient = new Patient("Abebe Kebede", LocalDate.of(1990, 5, 1), "abebe", "secret", "0911000000");
        Slot slot = new Slot(FIXED_NOW.plusHours(1));
        Appointment appointment = new Appointment(
                patient, slot, AppointmentStatus.CONFIRMED, FeeCategory.ADULT, new BigDecimal("250"), FIXED_NOW);
        setId(appointment, 1L);
        return appointment;
    }

    private QueueEntry waitingEntry() {
        return new QueueEntry(confirmedAppointment(), FIXED_NOW);
    }

    // Appointment.id is assigned by JPA (@GeneratedValue), so it's null on a plain "new
    // Appointment(...)" - reflection sets it here purely so appointmentService.markAttended(id)
    // has a real, assertable value to be verified against, mirroring what a real save() would do.
    private void setId(Appointment appointment, Long id) {
        try {
            var field = Appointment.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(appointment, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
