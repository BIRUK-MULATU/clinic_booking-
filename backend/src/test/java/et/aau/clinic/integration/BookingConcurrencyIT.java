package et.aau.clinic.integration;

import et.aau.clinic.domain.AppointmentStatus;
import et.aau.clinic.domain.Patient;
import et.aau.clinic.domain.RejectionReason;
import et.aau.clinic.domain.Slot;
import et.aau.clinic.repository.AppointmentRepository;
import et.aau.clinic.repository.PatientRepository;
import et.aau.clinic.repository.SlotRepository;
import et.aau.clinic.service.AppointmentService;
import et.aau.clinic.service.BookingOutcome;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DEF-005 - concurrency test for the classic check-then-act race on
 * Rule 2's C1 ("is the slot free?"). Eight patients hammer the same
 * slot at once; without the pessimistic lock added to
 * SlotRepository.findByIdForUpdate, two or more of them read the slot
 * as free before either has inserted, and the slot ends up
 * double-booked. With the lock, the critical section is serialised and
 * exactly one booking wins.
 *
 * Deliberately NOT @Transactional: each requestBooking call has to
 * commit in its own transaction for the lock and the race to be real,
 * so this class cleans up after itself instead.
 */
@SpringBootTest
class BookingConcurrencyIT {

    private static final LocalDateTime SLOT_TIME = LocalDateTime.now().plusDays(3).withNano(0);
    private static final int CONTENDERS = 8;

    @Autowired
    private AppointmentService appointmentService;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private SlotRepository slotRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;

    @AfterEach
    void cleanUp() {
        appointmentRepository.deleteAll();
        patientRepository.deleteAll();
        slotRepository.deleteAll();
    }

    @Test
    void eightPatientsBookingTheSameSlotAtOnce_produceExactlyOneAppointment() throws Exception {
        Slot slot = slotRepository.save(new Slot(SLOT_TIME));
        List<Long> patientIds = new java.util.ArrayList<>();
        for (int i = 0; i < CONTENDERS; i++) {
            Patient p = patientRepository.save(new Patient(
                    "Racer " + i, LocalDate.of(1990, 1, 1), "racer-" + i, "secret", "091100000" + i));
            patientIds.add(p.getId());
        }

        ExecutorService pool = Executors.newFixedThreadPool(CONTENDERS);
        CountDownLatch startGun = new CountDownLatch(1);
        List<Future<BookingOutcome>> futures = new java.util.ArrayList<>();
        for (Long patientId : patientIds) {
            Callable<BookingOutcome> task = () -> {
                startGun.await();
                return appointmentService.requestBooking(patientId, slot.getId());
            };
            futures.add(pool.submit(task));
        }
        startGun.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(20, TimeUnit.SECONDS)).isTrue();

        int approved = 0;
        int rejectedAsUnavailable = 0;
        for (Future<BookingOutcome> future : futures) {
            BookingOutcome outcome = future.get();
            if (outcome.decision().isApproved()) {
                approved++;
            } else if (outcome.decision().getReason() == RejectionReason.SLOT_UNAVAILABLE) {
                rejectedAsUnavailable++;
            }
        }

        assertThat(approved).as("exactly one booking should win the race").isEqualTo(1);
        assertThat(rejectedAsUnavailable).isEqualTo(CONTENDERS - 1);
        assertThat(appointmentRepository.findAll().stream()
                .filter(a -> a.getStatus() == AppointmentStatus.REQUESTED)
                .count()).isEqualTo(1);
    }
}
