package et.aau.clinic.integration;

import et.aau.clinic.domain.Appointment;
import et.aau.clinic.domain.AppointmentStatus;
import et.aau.clinic.domain.FeeCategory;
import et.aau.clinic.domain.Patient;
import et.aau.clinic.domain.Slot;
import et.aau.clinic.repository.AppointmentRepository;
import et.aau.clinic.repository.PatientRepository;
import et.aau.clinic.repository.SlotRepository;
import et.aau.clinic.service.AppointmentService;
import et.aau.clinic.service.BookingOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Pairwise (all-pairs) testing of self-service booking (requestBooking),
 * over the six independent factors that decide the outcome:
 *
 *   age band    : CHILD | ADULT | SENIOR          (Rule 1, sets the fee)
 *   slot free   : FREE | TAKEN                    (Rule 2 C1)
 *   balance     : CLEAR | OWING                   (Rule 2 C2)
 *   notice      : ENOUGH (>= 2h) | SHORT          (Rule 2 C3)
 *   suspension  : CLEAR | SUSPENDED (3 no-shows)  (Rule H, checked first)
 *   coverage    : 0% | 50% | 100%                 (Rule G, sets net payable)
 *
 * Full Cartesian product = 3 x 2 x 2 x 2 x 2 x 3 = 144 combinations. The
 * table below is a 10-row covering array (generated greedily) in which
 * every pair of factor values co-occurs in at least one row - the
 * standard all-pairs coverage criterion - seeded with the three
 * approve-path rows so each age x coverage fee combination is also
 * exercised on a successful booking. One supplementary row (TC-P11) is
 * added beyond the covering array so OUTSTANDING_BALANCE appears as an
 * actually-reported reason, not just as a factor value that a
 * higher-priority condition masks.
 *
 * Expected outcome per row follows the priority order Rule H -> C1 -> C2
 * -> C3; on approval the fee is Rule 1's and net payable is Rule G's.
 *
 * Real data, real H2, fixed Clock - the same wiring as AppointmentServiceIT.
 */
@SpringBootTest
@Transactional
class BookingPairwiseIT {

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

    private final AtomicInteger seq = new AtomicInteger();

    enum Age { CHILD, ADULT, SENIOR }

    /** null approvedReason means "approve"; feeCategory/net are only checked when approved. */
    record Row(String id, Age age, boolean slotFree, boolean balanceClear, boolean noticeEnough,
               boolean notSuspended, int coverage,
               String reason, FeeCategory feeCategory, String netPayable) {
    }

    static Stream<Arguments> pairwiseRows() {
        return Stream.of(
                // --- covering array: seeded approve-path rows (age x coverage) ---
                arguments(new Row("TC-P01", Age.CHILD, true, true, true, true, 0,
                        null, FeeCategory.CHILD, "100.00")),
                arguments(new Row("TC-P02", Age.ADULT, true, true, true, true, 50,
                        null, FeeCategory.ADULT, "125.00")),
                arguments(new Row("TC-P03", Age.SENIOR, true, true, true, true, 100,
                        null, FeeCategory.SENIOR, "0.00")),
                // --- covering array: greedy fill ---
                arguments(new Row("TC-P04", Age.CHILD, false, false, false, false, 50,
                        "SUSPENDED_NO_SHOWS", null, null)),
                arguments(new Row("TC-P05", Age.ADULT, true, false, false, false, 0,
                        "SUSPENDED_NO_SHOWS", null, null)),
                arguments(new Row("TC-P06", Age.SENIOR, false, true, false, false, 100,
                        "SUSPENDED_NO_SHOWS", null, null)),
                arguments(new Row("TC-P07", Age.ADULT, false, false, true, true, 100,
                        "SLOT_UNAVAILABLE", null, null)),
                arguments(new Row("TC-P08", Age.SENIOR, false, false, true, false, 0,
                        "SUSPENDED_NO_SHOWS", null, null)),
                arguments(new Row("TC-P09", Age.CHILD, true, true, false, true, 100,
                        "INSUFFICIENT_NOTICE", null, null)),
                arguments(new Row("TC-P10", Age.SENIOR, true, true, true, true, 50,
                        null, FeeCategory.SENIOR, "75.00")),
                // --- supplement: force OUTSTANDING_BALANCE as the reported reason ---
                arguments(new Row("TC-P11", Age.ADULT, true, false, true, true, 0,
                        "OUTSTANDING_BALANCE", null, null))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pairwiseRows")
    void requestBooking_pairwise(Row row) {
        int n = seq.incrementAndGet();
        Patient patient = new Patient("Pairwise " + n, dobFor(row.age()),
                "pw-" + n, "secret", "09110000" + n);
        patient.setCoveragePercent(row.coverage());
        if (!row.balanceClear()) {
            patient.setOutstandingBalance(new BigDecimal("100"));
        }
        patient = patientRepository.save(patient);

        if (!row.notSuspended()) {
            giveThreeNoShows(patient);
        }

        LocalDateTime slotTime = row.noticeEnough() ? FIXED_NOW.plusHours(5) : FIXED_NOW.plusMinutes(30);
        Slot target = slotRepository.save(new Slot(slotTime));
        if (!row.slotFree()) {
            occupy(target);
        }

        BookingOutcome outcome = appointmentService.requestBooking(patient.getId(), target.getId());

        if (row.reason() == null) {
            assertThat(outcome.decision().isApproved()).as("%s should be approved", row.id()).isTrue();
            assertThat(outcome.appointment().getFeeCategory()).isEqualTo(row.feeCategory());
            assertThat(outcome.appointment().getNetPayable()).isEqualByComparingTo(row.netPayable());
        } else {
            assertThat(outcome.decision().isApproved()).as("%s should be rejected", row.id()).isFalse();
            assertThat(outcome.decision().getReason().name()).isEqualTo(row.reason());
            assertThat(outcome.appointment()).isNull();
        }
    }

    private LocalDate dobFor(Age age) {
        return switch (age) {
            case CHILD -> FIXED_NOW.toLocalDate().minusYears(10);
            case ADULT -> FIXED_NOW.toLocalDate().minusYears(40);
            case SENIOR -> FIXED_NOW.toLocalDate().minusYears(70);
        };
    }

    private void giveThreeNoShows(Patient patient) {
        for (int i = 1; i <= 3; i++) {
            Slot past = slotRepository.save(new Slot(FIXED_NOW.minusDays(i)));
            appointmentRepository.save(new Appointment(patient, past, AppointmentStatus.NO_SHOW,
                    FeeCategory.ADULT, new BigDecimal("250"), FIXED_NOW.minusDays(i + 1)));
        }
    }

    private void occupy(Slot slot) {
        Patient other = patientRepository.save(new Patient("Holder " + seq.incrementAndGet(),
                LocalDate.of(1990, 1, 1), "holder-" + seq.get(), "secret", "0912000000"));
        appointmentRepository.save(new Appointment(other, slot, AppointmentStatus.CONFIRMED,
                FeeCategory.ADULT, new BigDecimal("250"), FIXED_NOW));
    }
}
