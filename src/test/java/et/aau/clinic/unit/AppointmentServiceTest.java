package et.aau.clinic.unit;

import et.aau.clinic.domain.Appointment;
import et.aau.clinic.domain.AppointmentStatus;
import et.aau.clinic.domain.FeeCategory;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Phase 4: exercises AppointmentService against the two testability
 * seams from CLAUDE.md - a Clock stub fixed at a known instant so the
 * 2h/24h boundaries can be hit exactly, and a Mockito mock of
 * NotificationService so a confirmation SMS can be verified without
 * sending one. The repositories are also Mockito mocks: this is a
 * unit test of the orchestration logic alone, not the database -
 * that combination is covered later by the *IT integration tests.
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 1, 10, 10, 0);
    private static final Clock FIXED_CLOCK =
            Clock.fixed(FIXED_NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Mock
    private PatientRepository patientRepository;
    @Mock
    private SlotRepository slotRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private NotificationService notificationService;

    private AppointmentService service;

    @BeforeEach
    void setUp() {
        service = new AppointmentService(
                patientRepository, slotRepository, appointmentRepository, FIXED_CLOCK, notificationService);
    }

    @Test
    void login_correctCredentials_returnsPatient() {
        Patient patient = adultPatient();
        when(patientRepository.findByUsername("abebe")).thenReturn(Optional.of(patient));

        Optional<Patient> result = service.login("abebe", "secret");

        assertThat(result).contains(patient);
    }

    @Test
    void login_wrongPassword_returnsEmpty() {
        Patient patient = adultPatient();
        when(patientRepository.findByUsername("abebe")).thenReturn(Optional.of(patient));

        Optional<Patient> result = service.login("abebe", "wrong");

        assertThat(result).isEmpty();
    }

    @Test
    void login_unknownUsername_returnsEmpty() {
        when(patientRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        Optional<Patient> result = service.login("ghost", "secret");

        assertThat(result).isEmpty();
    }

    @Test
    void requestBooking_allConditionsMet_createsRequestedAppointmentWithFee() {
        Patient patient = adultPatient();
        Slot slot = new Slot(FIXED_NOW.plusHours(3));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(slotRepository.findById(2L)).thenReturn(Optional.of(slot));
        when(appointmentRepository.existsBySlotAndStatusIn(eq(slot), any())).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingOutcome outcome = service.requestBooking(1L, 2L);

        assertThat(outcome.decision().isApproved()).isTrue();
        assertThat(outcome.appointment().getStatus()).isEqualTo(AppointmentStatus.REQUESTED);
        assertThat(outcome.appointment().getFeeCategory()).isEqualTo(FeeCategory.ADULT);
        assertThat(outcome.appointment().getFeeAmount()).isEqualByComparingTo(new BigDecimal("250"));
    }

    @Test
    void requestBooking_slotAlreadyTaken_rejectsAndSavesNothing() {
        Patient patient = adultPatient();
        Slot slot = new Slot(FIXED_NOW.plusHours(3));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(slotRepository.findById(2L)).thenReturn(Optional.of(slot));
        when(appointmentRepository.existsBySlotAndStatusIn(eq(slot), any())).thenReturn(true);

        BookingOutcome outcome = service.requestBooking(1L, 2L);

        assertThat(outcome.decision().isApproved()).isFalse();
        assertThat(outcome.decision().getReason()).isEqualTo(RejectionReason.SLOT_UNAVAILABLE);
        assertThat(outcome.appointment()).isNull();
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void requestBooking_outstandingBalance_rejects() {
        Patient patient = adultPatient();
        patient.setOutstandingBalance(new BigDecimal("50"));
        Slot slot = new Slot(FIXED_NOW.plusHours(3));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(slotRepository.findById(2L)).thenReturn(Optional.of(slot));
        when(appointmentRepository.existsBySlotAndStatusIn(eq(slot), any())).thenReturn(false);

        BookingOutcome outcome = service.requestBooking(1L, 2L);

        assertThat(outcome.decision().getReason()).isEqualTo(RejectionReason.OUTSTANDING_BALANCE);
    }

    @Test
    void requestBooking_lessThanTwoHoursNotice_rejects() {
        Patient patient = adultPatient();
        Slot slot = new Slot(FIXED_NOW.plusMinutes(30));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(slotRepository.findById(2L)).thenReturn(Optional.of(slot));
        when(appointmentRepository.existsBySlotAndStatusIn(eq(slot), any())).thenReturn(false);

        BookingOutcome outcome = service.requestBooking(1L, 2L);

        assertThat(outcome.decision().getReason()).isEqualTo(RejectionReason.INSUFFICIENT_NOTICE);
    }

    @Test
    void confirm_requestedAppointment_movesToConfirmedAndSendsNotification() {
        Patient patient = adultPatient();
        Slot slot = new Slot(FIXED_NOW.plusHours(3));
        Appointment appointment = new Appointment(
                patient, slot, AppointmentStatus.REQUESTED, FeeCategory.ADULT, new BigDecimal("250"), FIXED_NOW);
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment result = service.confirm(5L);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        verify(notificationService).sendConfirmation(patient, result);
    }

    @Test
    void cancel_confirmedAppointmentWithinTwentyFourHours_chargesHalfFee() {
        Patient patient = adultPatient();
        Slot slot = new Slot(FIXED_NOW.plusHours(10));
        Appointment appointment = new Appointment(
                patient, slot, AppointmentStatus.CONFIRMED, FeeCategory.ADULT, new BigDecimal("250"), FIXED_NOW);
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment result = service.cancel(5L);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(result.getCancellationFee()).isEqualByComparingTo(new BigDecimal("125.0"));
    }

    @Test
    void cancel_confirmedAppointmentTwentyFourHoursOrMoreOut_isFree() {
        Patient patient = adultPatient();
        Slot slot = new Slot(FIXED_NOW.plusHours(48));
        Appointment appointment = new Appointment(
                patient, slot, AppointmentStatus.CONFIRMED, FeeCategory.ADULT, new BigDecimal("250"), FIXED_NOW);
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment result = service.cancel(5L);

        assertThat(result.getCancellationFee()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void cancel_requestedAppointment_hasNoCancellationFee() {
        Patient patient = adultPatient();
        Slot slot = new Slot(FIXED_NOW.plusHours(48));
        Appointment appointment = new Appointment(
                patient, slot, AppointmentStatus.REQUESTED, FeeCategory.ADULT, new BigDecimal("250"), FIXED_NOW);
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment result = service.cancel(5L);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(result.getCancellationFee()).isNull();
    }

    @Test
    void listAvailableSlots_excludesTakenAndPastSlots() {
        Slot future = new Slot(FIXED_NOW.plusHours(5));
        Slot taken = new Slot(FIXED_NOW.plusHours(6));
        Slot past = new Slot(FIXED_NOW.minusHours(1));
        when(slotRepository.findAllByOrderByStartTimeAsc()).thenReturn(List.of(future, taken, past));
        when(appointmentRepository.existsBySlotAndStatusIn(eq(future), any())).thenReturn(false);
        when(appointmentRepository.existsBySlotAndStatusIn(eq(taken), any())).thenReturn(true);

        List<Slot> available = service.listAvailableSlots();

        assertThat(available).containsExactly(future);
    }

    private Patient adultPatient() {
        return new Patient("Abebe Kebede", LocalDate.of(1990, 5, 1), "abebe", "secret", "0911000000");
    }
}
