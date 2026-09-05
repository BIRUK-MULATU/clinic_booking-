package et.aau.clinic.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import java.time.LocalDateTime;

/**
 * One patient's day-of-visit queue entry (hospital-expansion Phase D),
 * created at check-in - a separate entity from Appointment because it
 * has a completely different lifetime: an Appointment exists for days
 * or weeks once booked, a QueueEntry exists for a few hours, only after
 * the patient physically arrives. checkedInAt is its own creation time,
 * exactly parallel to how Appointment.requestedAt is Appointment's.
 *
 * @OneToOne with a unique join column: at most one queue entry per
 * appointment, enforced at the schema level, not just by convention.
 */
@Entity
public class QueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(nullable = false, unique = true)
    private Appointment appointment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueueEntryStatus status;

    @Column(nullable = false)
    private LocalDateTime checkedInAt;

    protected QueueEntry() {
        // required by JPA
    }

    public QueueEntry(Appointment appointment, LocalDateTime checkedInAt) {
        this.appointment = appointment;
        this.checkedInAt = checkedInAt;
        this.status = QueueEntryStatus.WAITING;
    }

    public Long getId() {
        return id;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public QueueEntryStatus getStatus() {
        return status;
    }

    public void setStatus(QueueEntryStatus status) {
        this.status = status;
    }

    public LocalDateTime getCheckedInAt() {
        return checkedInAt;
    }
}
