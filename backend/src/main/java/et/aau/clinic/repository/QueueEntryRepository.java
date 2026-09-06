package et.aau.clinic.repository;

import et.aau.clinic.domain.QueueEntry;
import et.aau.clinic.domain.QueueEntryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

    // FIFO ordering by check-in time, same trick as Phase C's waitlist promotion -
    // "who's next" reuses an existing creation timestamp rather than a new field.
    List<QueueEntry> findByStatusOrderByCheckedInAtAsc(QueueEntryStatus status);

    // For a front-desk-style "current queue" view spanning every non-terminal status.
    List<QueueEntry> findByStatusInOrderByCheckedInAtAsc(Collection<QueueEntryStatus> statuses);
}
