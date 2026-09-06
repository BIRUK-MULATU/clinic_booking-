package et.aau.clinic.repository;

import et.aau.clinic.domain.Doctor;
import et.aau.clinic.domain.Slot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SlotRepository extends JpaRepository<Slot, Long> {

    List<Slot> findAllByOrderByStartTimeAsc();

    /**
     * DEF-005: takes a row-level write lock on the slot so the "is this slot free?" check and
     * the appointment insert that follows it run as one critical section. Two bookings racing
     * for the same slot are serialised - the second waits for the first to commit, then sees
     * its appointment and is rejected with SLOT_UNAVAILABLE instead of also being created.
     * Only used on the self-service and reception booking paths, which is where the race is.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Slot s where s.id = :id")
    Optional<Slot> findByIdForUpdate(@Param("id") Long id);

    List<Slot> findByDoctorAndStartTimeBetween(Doctor doctor, LocalDateTime from, LocalDateTime to);

    List<Slot> findByDoctor(Doctor doctor);
}
