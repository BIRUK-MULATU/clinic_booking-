package et.aau.clinic.repository;

import et.aau.clinic.domain.Doctor;
import et.aau.clinic.domain.Slot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SlotRepository extends JpaRepository<Slot, Long> {

    List<Slot> findAllByOrderByStartTimeAsc();

    List<Slot> findByDoctorAndStartTimeBetween(Doctor doctor, LocalDateTime from, LocalDateTime to);
}
