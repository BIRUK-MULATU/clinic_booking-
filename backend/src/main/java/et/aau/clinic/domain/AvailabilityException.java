package et.aau.clinic.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.LocalDate;

/**
 * A whole-day leave/holiday override for one doctor: no slots are
 * generated on this date, regardless of what the weekly rules say.
 * Partial-day exceptions are deliberately out of scope - nobody asked
 * for them, and they would be real added complexity.
 */
@Entity
public class AvailabilityException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private Doctor doctor;

    @Column(nullable = false)
    private LocalDate date;

    protected AvailabilityException() {
        // required by JPA
    }

    public AvailabilityException(Doctor doctor, LocalDate date) {
        this.doctor = doctor;
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public LocalDate getDate() {
        return date;
    }
}
