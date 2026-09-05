package et.aau.clinic.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

/**
 * A bookable appointment time. Deliberately has no "free"/"booked" flag:
 * whether a slot is free (C1 in Rule 2) is derived by the service layer
 * from whether an active Appointment already references it, so there is
 * one source of truth instead of two that can drift out of sync.
 */
@Entity
public class Slot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime startTime;

    protected Slot() {
        // required by JPA
    }

    public Slot(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
}
