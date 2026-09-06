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
    void bookForPatient_freeSlot_createsConfirmedAppointmentAndSendsSms() {
        Patient patient = adultPatient();
        Slot slot = new Slot(FIXED_NOW.plusMinutes(30)); // inside the 2h window: reception may override C3
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(slotRepository.findById(2L)).thenReturn(Optional.of(slot));
        when(appointmentRepository.existsBySlotAndStatusIn(eq(slot), any())).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingOutcome outcome = service.bookForPatient(1L, 2L);

        assertThat(outcome.decision().isApproved()).isTrue();
        assertThat(outcome.appointment().getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(outcome.appointment().getFeeAmount()).isEqualByComparingTo(new BigDecimal("250"));
        verify(notificationService).sendConfirmation(eq(patient), any(Appointment.class));
    }

    @Test
    void bookForPatient_outstandingBalanceAndShortNotice_stillBooks_receptionOverridesC2AndC3() {
        Patient patient = adultPatient();
        patient.setOutstandingBalance(new BigDecimal("500"));
        Slot slot = new Slot(FIXED_NOW.plusMinutes(15));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(slotRepository.findById(2L)).thenReturn(Optional.of(slot));
        when(appointmentRepository.existsBySlotAndStatusIn(eq(slot), any())).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingOutcome outcome = service.bookForPatient(1L, 2L);

        assertThat(outcome.decision().isApproved()).isTrue();
        assertThat(outcome.appointment().getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    void bookForPatient_slotAlreadyTaken_rejectsWithSlotUnavailableAndSavesNothing() {
        Patient patient = adultPatient();
        Slot slot = new Slot(FIXED_NOW.plusHours(3));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(slotRepository.findById(2L)).thenReturn(Optional.of(slot));
        when(appointmentRepository.existsBySlotAndStatusIn(eq(slot), any())).thenReturn(true);

        BookingOutcome outcome = service.bookForPatient(1L, 2L);

        assertThat(outcome.decision().isApproved()).isFalse();
        assertThat(outcome.decision().getReason()).isEqualTo(RejectionReason.SLOT_UNAVAILABLE);
        assertThat(outcome.appointment()).isNull();
        verify(appointmentRepository, never()).save(any());
        verify(notificationService, never()).sendConfirmation(any(), any());
    }

    @Test
    void listUpcomingSlots_includesTakenSlots_butNotPastOnes() {
        Slot future = new Slot(FIXED_NOW.plusHours(5));
        Slot taken = new Slot(FIXED_NOW.plusHours(6));
        Slot past = new Slot(FIXED_NOW.minusHours(1));
        when(slotRepository.findAllByOrderByStartTimeAsc()).thenReturn(List.of(future, taken, past));

        List<Slot> upcoming = service.listUpcomingSlots();

        assertThat(upcoming).containsExactly(future, taken);
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

    // --- Rule F: the 24-hour reminder sweep -------------------------------------------------
    // ReminderPolicy's own boundaries are covered in ReminderPolicyTest; these check that the
    // service pulls the right rows, sends via the seam, and stamps reminderSentAt so it never
    // double-sends. The Clock stub fixes "now" so "within 24h" is exact.

    @Test
    void sendDueReminders_confirmedAppointmentWithinWindow_sendsReminderAndStampsIt() {
        Patient patient = adultPatient();
        Slot slot = new Slot(FIXED_NOW.plusHours(5));
        Appointment appointment = new Appointment(
                patient, slot, AppointmentStatus.CONFIRMED, FeeCategory.ADULT, new BigDecimal("250"), FIXED_NOW);
        when(appointmentRepository.findByStatusAndReminderSentAtIsNull(AppointmentStatus.CONFIRMED))
                .thenReturn(List.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int sent = service.sendDueReminders();

        assertThat(sent).isEqualTo(1);
        verify(notificationService).sendReminder(patient, appointment);
        assertThat(appointment.getReminderSentAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    void sendDueReminders_confirmedAppointmentStillOutsideWindow_sendsNothing() {
        Patient patient = adultPatient();
        Slot slot = new Slot(FIXED_NOW.plusHours(48)); // more than 24h away
        Appointment appointment = new Appointment(
                patient, slot, AppointmentStatus.CONFIRMED, FeeCategory.ADULT, new BigDecimal("250"), FIXED_NOW);
        when(appointmentRepository.findByStatusAndReminderSentAtIsNull(AppointmentStatus.CONFIRMED))
                .thenReturn(List.of(appointment));

        int sent = service.sendDueReminders();

        assertThat(sent).isZero();
        verify(notificationService, never()).sendReminder(any(), any());
        verify(appointmentRepository, never()).save(any());
        assertThat(appointment.getReminderSentAt()).isNull();
    }

    @Test
    void sendDueReminders_twoConfirmedAppointments_remindsOnlyTheOneInsideTheWindow() {
        Patient soon = adultPatient();
        Appointment dueNow = new Appointment(soon, new Slot(FIXED_NOW.plusHours(3)),
                AppointmentStatus.CONFIRMED, FeeCategory.ADULT, new BigDecimal("250"), FIXED_NOW);
        Patient later = new Patient("Sara Nega", LocalDate.of(1988, 2, 2), "sara", "secret", "0911222333");
        Appointment notYet = new Appointment(later, new Slot(FIXED_NOW.plusDays(3)),
                AppointmentStatus.CONFIRMED, FeeCategory.ADULT, new BigDecimal("250"), FIXED_NOW);
        when(appointmentRepository.findByStatusAndReminderSentAtIsNull(AppointmentStatus.CONFIRMED))
                .thenReturn(List.of(dueNow, notYet));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int sent = service.sendDueReminders();

        assertThat(sent).isEqualTo(1);
        verify(notificationService).sendReminder(soon, dueNow);
        verify(notificationService, never()).sendReminder(eq(later), any());
    }

    // Rule H: a patient with 3+ recent no-shows is barred from self-booking, but reception can
    // still book them in.
    @Test
    void requestBooking_patientSuspendedForThreeNoShows_rejectsWithSuspendedReasonAndSavesNothing() {
        Patient patient = adultPatient();
        Slot slot = new Slot(FIXED_NOW.plusHours(3));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(slotRepository.findById(2L)).thenReturn(Optional.of(slot));
        when(appointmentRepository.findByPatientAndStatus(patient, AppointmentStatus.NO_SHOW))
                .thenReturn(List.of(
                        noShow(patient, FIXED_NOW.minusDays(5)),
                        noShow(patient, FIXED_NOW.minusDays(20)),
                        noShow(patient, FIXED_NOW.minusDays(50))));

        BookingOutcome outcome = service.requestBooking(1L, 2L);

        assertThat(outcome.decision().isApproved()).isFalse();
        assertThat(outcome.decision().getReason()).isEqualTo(RejectionReason.SUSPENDED_NO_SHOWS);
        assertThat(outcome.appointment()).isNull();
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void requestBooking_patientWithOnlyTwoRecentNoShows_isNotSuspended() {
        Patient patient = adultPatient();
        Slot slot = new Slot(FIXED_NOW.plusHours(3));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(slotRepository.findById(2L)).thenReturn(Optional.of(slot));
        when(appointmentRepository.findByPatientAndStatus(patient, AppointmentStatus.NO_SHOW))
                .thenReturn(List.of(
                        noShow(patient, FIXED_NOW.minusDays(5)),
                        noShow(patient, FIXED_NOW.minusDays(20))));
        when(appointmentRepository.existsBySlotAndStatusIn(eq(slot), any())).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingOutcome outcome = service.requestBooking(1L, 2L);

        assertThat(outcome.decision().isApproved()).isTrue();
    }

    @Test
    void bookForPatient_receptionPath_neverConsultsNoShowHistory() {
        Patient patient = adultPatient();
        Slot slot = new Slot(FIXED_NOW.plusHours(3));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(slotRepository.findById(2L)).thenReturn(Optional.of(slot));
        when(appointmentRepository.existsBySlotAndStatusIn(eq(slot), any())).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingOutcome outcome = service.bookForPatient(1L, 2L);

        assertThat(outcome.decision().isApproved()).isTrue();
        verify(appointmentRepository, never()).findByPatientAndStatus(any(), any());
    }

    // Rule I: reschedule moves the slot, keeps the state, and reprices from the patient's age today.
    @Test
    void reschedule_requestedAppointment_toAFreeSlot_movesSlotAndKeepsRequestedState() {
        Patient patient = adultPatient();
        Slot oldSlot = new Slot(FIXED_NOW.plusHours(5));
        Slot newSlot = new Slot(FIXED_NOW.plusDays(2));
        Appointment appointment = new Appointment(
                patient, oldSlot, AppointmentStatus.REQUESTED, FeeCategory.ADULT, new BigDecimal("250"), FIXED_NOW);
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));
        when(slotRepository.findById(9L)).thenReturn(Optional.of(newSlot));
        when(appointmentRepository.existsBySlotAndStatusIn(eq(newSlot), any())).thenReturn(false);
        when(appointmentRepository.findFirstBySlotAndStatusOrderByRequestedAtAsc(oldSlot, AppointmentStatus.WAITLISTED))
                .thenReturn(Optional.empty());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var outcome = service.reschedule(5L, 9L);

        assertThat(outcome.decision().isApproved()).isTrue();
        assertThat(outcome.appointment().getSlot()).isSameAs(newSlot);
        assertThat(outcome.appointment().getStatus()).isEqualTo(AppointmentStatus.REQUESTED);
    }

    @Test
    void reschedule_childWhoHasSinceTurned18_isRepricedToTheAdultBand() {
        // DOB exactly 18 years before FIXED_NOW: a CHILD booking is repriced to ADULT on reschedule.
        Patient patient = new Patient("Grown Up", FIXED_NOW.toLocalDate().minusYears(18),
                "grown", "secret", "0911999888");
        Slot newSlot = new Slot(FIXED_NOW.plusDays(2));
        Appointment appointment = new Appointment(patient, new Slot(FIXED_NOW.plusHours(5)),
                AppointmentStatus.CONFIRMED, FeeCategory.CHILD, new BigDecimal("100"), FIXED_NOW.minusDays(400));
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));
        when(slotRepository.findById(9L)).thenReturn(Optional.of(newSlot));
        when(appointmentRepository.existsBySlotAndStatusIn(eq(newSlot), any())).thenReturn(false);
        when(appointmentRepository.findFirstBySlotAndStatusOrderByRequestedAtAsc(any(), eq(AppointmentStatus.WAITLISTED)))
                .thenReturn(Optional.empty());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var outcome = service.reschedule(5L, 9L);

        assertThat(outcome.appointment().getFeeCategory()).isEqualTo(FeeCategory.ADULT);
        assertThat(outcome.appointment().getFeeAmount()).isEqualByComparingTo("250");
    }

    @Test
    void reschedule_toATakenSlot_rejectsAndDoesNotMove() {
        Patient patient = adultPatient();
        Slot oldSlot = new Slot(FIXED_NOW.plusHours(5));
        Slot newSlot = new Slot(FIXED_NOW.plusDays(2));
        Appointment appointment = new Appointment(
                patient, oldSlot, AppointmentStatus.REQUESTED, FeeCategory.ADULT, new BigDecimal("250"), FIXED_NOW);
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));
        when(slotRepository.findById(9L)).thenReturn(Optional.of(newSlot));
        when(appointmentRepository.existsBySlotAndStatusIn(eq(newSlot), any())).thenReturn(true);

        var outcome = service.reschedule(5L, 9L);

        assertThat(outcome.decision().isApproved()).isFalse();
        assertThat(outcome.decision().getReason())
                .isEqualTo(et.aau.clinic.domain.RescheduleRejection.SLOT_UNAVAILABLE);
        assertThat(appointment.getSlot()).isSameAs(oldSlot);
        verify(appointmentRepository, never()).save(any());
    }

    // Rule J: the waitlist-offer expiry sweep - an expired offer lapses to OFFER_EXPIRED and the
    // slot is offered to the next person on the waitlist.
    @Test
    void expireStaleWaitlistOffers_offerOlderThanTwoHours_lapsesToOfferExpiredAndPromotesNext() {
        Patient patient = adultPatient();
        Slot slot = new Slot(FIXED_NOW.plusDays(1));
        Appointment offer = new Appointment(
                patient, slot, AppointmentStatus.REQUESTED, FeeCategory.ADULT, new BigDecimal("250"), FIXED_NOW);
        offer.setWaitlistOfferedAt(FIXED_NOW.minusHours(3));
        Appointment nextInLine = new Appointment(patient, slot,
                AppointmentStatus.WAITLISTED, FeeCategory.ADULT, new BigDecimal("250"), FIXED_NOW);
        when(appointmentRepository.findByStatusAndWaitlistOfferedAtIsNotNull(AppointmentStatus.REQUESTED))
                .thenReturn(List.of(offer));
        when(appointmentRepository.findFirstBySlotAndStatusOrderByRequestedAtAsc(slot, AppointmentStatus.WAITLISTED))
                .thenReturn(Optional.of(nextInLine));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int expired = service.expireStaleWaitlistOffers();

        assertThat(expired).isEqualTo(1);
        assertThat(offer.getStatus()).isEqualTo(AppointmentStatus.OFFER_EXPIRED);
        assertThat(nextInLine.getStatus()).isEqualTo(AppointmentStatus.REQUESTED);
        assertThat(nextInLine.getWaitlistOfferedAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    void expireStaleWaitlistOffers_offerStillWithinTwoHours_isLeftAlone() {
        Patient patient = adultPatient();
        Appointment offer = new Appointment(patient, new Slot(FIXED_NOW.plusDays(1)),
                AppointmentStatus.REQUESTED, FeeCategory.ADULT, new BigDecimal("250"), FIXED_NOW);
        offer.setWaitlistOfferedAt(FIXED_NOW.minusHours(1));
        when(appointmentRepository.findByStatusAndWaitlistOfferedAtIsNotNull(AppointmentStatus.REQUESTED))
                .thenReturn(List.of(offer));

        int expired = service.expireStaleWaitlistOffers();

        assertThat(expired).isZero();
        assertThat(offer.getStatus()).isEqualTo(AppointmentStatus.REQUESTED);
        verify(appointmentRepository, never()).save(any());
    }

    private Appointment noShow(Patient patient, LocalDateTime slotStart) {
        return new Appointment(patient, new Slot(slotStart), AppointmentStatus.NO_SHOW,
                FeeCategory.ADULT, new BigDecimal("250"), slotStart.minusDays(1));
    }

    // Rule G: the net payable after insurance is captured on the appointment at booking time.
    @Test
    void requestBooking_patientWith40PercentCoverage_capturesNetPayableOnTheAppointment() {
        Patient patient = adultPatient();
        patient.setCoveragePercent(40);
        Slot slot = new Slot(FIXED_NOW.plusHours(3));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(slotRepository.findById(2L)).thenReturn(Optional.of(slot));
        when(appointmentRepository.existsBySlotAndStatusIn(eq(slot), any())).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingOutcome outcome = service.requestBooking(1L, 2L);

        assertThat(outcome.appointment().getFeeAmount()).isEqualByComparingTo("250");
        assertThat(outcome.appointment().getNetPayable()).isEqualByComparingTo("150.00"); // 250 - 40%
    }

    @Test
    void bookForPatient_patientWithNoCoverage_netPayableEqualsFee() {
        Patient patient = adultPatient();
        Slot slot = new Slot(FIXED_NOW.plusHours(3));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(slotRepository.findById(2L)).thenReturn(Optional.of(slot));
        when(appointmentRepository.existsBySlotAndStatusIn(eq(slot), any())).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingOutcome outcome = service.bookForPatient(1L, 2L);

        assertThat(outcome.appointment().getNetPayable()).isEqualByComparingTo("250.00");
    }

    @Test
    void remind_confirmedAppointmentWithinWindow_sendsAndReportsDue() {
        Patient patient = adultPatient();
        Slot slot = new Slot(FIXED_NOW.plusHours(4));
        Appointment appointment = new Appointment(
                patient, slot, AppointmentStatus.CONFIRMED, FeeCategory.ADULT, new BigDecimal("250"), FIXED_NOW);
        when(appointmentRepository.findById(9L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var decision = service.remind(9L);

        assertThat(decision.isDue()).isTrue();
        verify(notificationService).sendReminder(patient, appointment);
        assertThat(appointment.getReminderSentAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    void remind_appointmentAlreadyReminded_sendsNothingAndReportsTheReason() {
        Patient patient = adultPatient();
        Appointment appointment = new Appointment(patient, new Slot(FIXED_NOW.plusHours(4)),
                AppointmentStatus.CONFIRMED, FeeCategory.ADULT, new BigDecimal("250"), FIXED_NOW);
        appointment.setReminderSentAt(FIXED_NOW.minusHours(1));
        when(appointmentRepository.findById(9L)).thenReturn(Optional.of(appointment));

        var decision = service.remind(9L);

        assertThat(decision.isDue()).isFalse();
        assertThat(decision.getReason()).isEqualTo(et.aau.clinic.domain.ReminderSkipReason.ALREADY_REMINDED);
        verify(notificationService, never()).sendReminder(any(), any());
        verify(appointmentRepository, never()).save(any());
    }

    private Patient adultPatient() {
        return new Patient("Abebe Kebede", LocalDate.of(1990, 5, 1), "abebe", "secret", "0911000000");
    }
}
