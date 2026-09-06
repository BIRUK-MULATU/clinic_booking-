package et.aau.clinic.unit;

import et.aau.clinic.domain.Appointment;
import et.aau.clinic.domain.AppointmentStatus;
import et.aau.clinic.domain.FeeCategory;
import et.aau.clinic.domain.Patient;
import et.aau.clinic.domain.Slot;
import et.aau.clinic.domain.VisitRecord;
import et.aau.clinic.domain.VisitRejection;
import et.aau.clinic.repository.AppointmentRepository;
import et.aau.clinic.repository.VisitRecordRepository;
import et.aau.clinic.service.VisitRecordOutcome;
import et.aau.clinic.service.VisitRecordService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Hospital-expansion Phase E: VisitRecordService against Mockito mocks
 * for both repositories, with the Clock stub fixed so recordedAt is an
 * assertable value. This test covers the service's orchestration - it
 * maps appointment status and "record already exists?" into the two
 * booleans VisitRecordPolicy needs, and only saves on approval. The
 * decision-table and boundary logic itself is VisitRecordPolicyTest's
 * job, not re-tested here.
 */
@ExtendWith(MockitoExtension.class)
class VisitRecordServiceTest {

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 1, 10, 11, 30);
    private static final Clock FIXED_CLOCK =
            Clock.fixed(FIXED_NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private VisitRecordRepository visitRecordRepository;

    private VisitRecordService service;

    @BeforeEach
    void setUp() {
        service = new VisitRecordService(appointmentRepository, visitRecordRepository, FIXED_CLOCK);
    }

    @Test
    void record_attendedAppointmentWithNoRecord_savesRecordAtClockTime() {
        Appointment appointment = appointment(AppointmentStatus.ATTENDED);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(visitRecordRepository.existsByAppointment(appointment)).thenReturn(false);
        when(visitRecordRepository.save(any(VisitRecord.class))).thenAnswer(i -> i.getArgument(0));

        VisitRecordOutcome outcome = service.record(1L, "  Sprained ankle  ", "  ", "Ibuprofen 400mg");

        assertThat(outcome.decision().isApproved()).isTrue();
        assertThat(outcome.record().getDiagnosis()).isEqualTo("Sprained ankle");
        assertThat(outcome.record().getNotes()).isNull();
        assertThat(outcome.record().getPrescription()).isEqualTo("Ibuprofen 400mg");
        assertThat(outcome.record().getRecordedAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    void record_appointmentNotAttended_isRejectedAndSavesNothing() {
        Appointment appointment = appointment(AppointmentStatus.CONFIRMED);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(visitRecordRepository.existsByAppointment(appointment)).thenReturn(false);

        VisitRecordOutcome outcome = service.record(1L, "Sprained ankle", null, null);

        assertThat(outcome.decision().isApproved()).isFalse();
        assertThat(outcome.decision().getReason()).isEqualTo(VisitRejection.APPOINTMENT_NOT_ATTENDED);
        assertThat(outcome.record()).isNull();
        verify(visitRecordRepository, never()).save(any());
    }

    @Test
    void record_recordAlreadyExists_isRejectedAndSavesNothing() {
        Appointment appointment = appointment(AppointmentStatus.ATTENDED);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(visitRecordRepository.existsByAppointment(appointment)).thenReturn(true);

        VisitRecordOutcome outcome = service.record(1L, "Sprained ankle", null, null);

        assertThat(outcome.decision().getReason()).isEqualTo(VisitRejection.VISIT_ALREADY_RECORDED);
        verify(visitRecordRepository, never()).save(any());
    }

    @Test
    void record_blankDiagnosis_isRejectedAndSavesNothing() {
        Appointment appointment = appointment(AppointmentStatus.ATTENDED);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(visitRecordRepository.existsByAppointment(appointment)).thenReturn(false);

        VisitRecordOutcome outcome = service.record(1L, "   ", "some notes", null);

        assertThat(outcome.decision().getReason()).isEqualTo(VisitRejection.DIAGNOSIS_REQUIRED);
        verify(visitRecordRepository, never()).save(any());
    }

    private Appointment appointment(AppointmentStatus status) {
        Patient patient = new Patient("Abebe Kebede", LocalDate.of(1990, 5, 1), "abebe", "secret", "0911000000");
        Slot slot = new Slot(FIXED_NOW.minusHours(1));
        return new Appointment(patient, slot, status, FeeCategory.ADULT, new BigDecimal("250"), FIXED_NOW.minusDays(1));
    }
}
