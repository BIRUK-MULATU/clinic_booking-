package et.aau.clinic.integration;

import et.aau.clinic.domain.AppointmentStatus;
import et.aau.clinic.domain.Patient;
import et.aau.clinic.domain.Slot;
import et.aau.clinic.domain.VisitRecord;
import et.aau.clinic.domain.VisitRejection;
import et.aau.clinic.repository.PatientRepository;
import et.aau.clinic.repository.SlotRepository;
import et.aau.clinic.repository.VisitRecordRepository;
import et.aau.clinic.service.AppointmentService;
import et.aau.clinic.service.BookingOutcome;
import et.aau.clinic.service.VisitRecordOutcome;
import et.aau.clinic.service.VisitRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hospital-expansion Phase E: VisitRecordService wired to real Spring
 * Data JPA repositories and a real H2 database instead of Mockito mocks.
 * This is what proves the repository query behind C2
 * (existsByAppointment) actually works, and that the @OneToOne unique
 * join column really does stop a second record for the same appointment
 * even if the policy check were somehow bypassed.
 *
 * @Transactional at class level purely for test-to-test row isolation,
 * the same reason AppointmentServiceIT uses it (see DEF-001). Nothing
 * here depends on a transaction rolling back mid-method.
 */
@SpringBootTest
@Transactional
class VisitRecordServiceIT {

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
    private VisitRecordService visitRecordService;
    @Autowired
    private AppointmentService appointmentService;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private SlotRepository slotRepository;
    @Autowired
    private VisitRecordRepository visitRecordRepository;

    @Test
    void record_attendedAppointment_persistsAndIsRetrievableByAppointment() {
        Long appointmentId = attendedAppointment("visithappy");

        VisitRecordOutcome outcome = visitRecordService.record(
                appointmentId, "Sprained left ankle, no fracture on X-ray", "Patient limping", "Ibuprofen 400mg TDS");

        assertThat(outcome.decision().isApproved()).isTrue();

        VisitRecord persisted = visitRecordRepository.findById(outcome.record().getId()).orElseThrow();
        assertThat(persisted.getAppointment().getId()).isEqualTo(appointmentId);
        assertThat(persisted.getDiagnosis()).isEqualTo("Sprained left ankle, no fracture on X-ray");
        assertThat(persisted.getRecordedAt()).isEqualTo(FIXED_NOW);
        assertThat(visitRecordService.findForAppointment(appointmentId)).isPresent();
    }

    @Test
    void record_secondAttemptForSameAppointment_isRejectedAsAlreadyRecorded() {
        Long appointmentId = attendedAppointment("visitdup");
        visitRecordService.record(appointmentId, "First diagnosis", null, null);

        VisitRecordOutcome second = visitRecordService.record(appointmentId, "Second diagnosis", null, null);

        assertThat(second.decision().isApproved()).isFalse();
        assertThat(second.decision().getReason()).isEqualTo(VisitRejection.VISIT_ALREADY_RECORDED);
        assertThat(visitRecordRepository.count()).isEqualTo(1);
    }

    @Test
    void record_confirmedButNotYetAttendedAppointment_isRejected() {
        Patient patient = patientRepository.save(
                new Patient("Tigist Alemu", LocalDate.of(1990, 1, 1), "visitnotattended", "secret", "0911111111"));
        Slot slot = slotRepository.save(new Slot(FIXED_NOW.plusHours(5)));
        BookingOutcome booking = appointmentService.requestBooking(patient.getId(), slot.getId());
        Long appointmentId = booking.appointment().getId();
        appointmentService.confirm(appointmentId);

        VisitRecordOutcome outcome = visitRecordService.record(appointmentId, "Diagnosis", null, null);

        assertThat(outcome.decision().getReason()).isEqualTo(VisitRejection.APPOINTMENT_NOT_ATTENDED);
        assertThat(visitRecordRepository.count()).isZero();
    }

    private Long attendedAppointment(String username) {
        Patient patient = patientRepository.save(
                new Patient("Tigist Alemu", LocalDate.of(1990, 1, 1), username, "secret", "0911111111"));
        Slot slot = slotRepository.save(new Slot(FIXED_NOW.plusHours(5)));
        BookingOutcome booking = appointmentService.requestBooking(patient.getId(), slot.getId());
        Long appointmentId = booking.appointment().getId();
        appointmentService.confirm(appointmentId);
        appointmentService.markAttended(appointmentId);
        assertThat(appointmentService.getAppointment(appointmentId).getStatus())
                .isEqualTo(AppointmentStatus.ATTENDED);
        return appointmentId;
    }
}
