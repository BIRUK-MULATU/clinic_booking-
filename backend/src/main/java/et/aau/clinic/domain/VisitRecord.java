package et.aau.clinic.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import java.time.LocalDateTime;

/**
 * The clinical record of one completed visit (hospital-expansion Phase
 * E): what the doctor found and prescribed, attached to the ATTENDED
 * appointment it belongs to.
 *
 * A separate entity from Appointment, linked @OneToOne with a unique
 * join column - exactly like QueueEntry (Phase D) - so "at most one
 * visit record per appointment" is enforced by the schema, not just by
 * VisitRecordPolicy's C2 check. recordedAt is its own creation time,
 * parallel to Appointment.requestedAt and QueueEntry.checkedInAt.
 *
 * diagnosis is mandatory (VisitRecordPolicy rejects a blank one);
 * notes and prescription are optional and left null when not given.
 * The column lengths are generous so a stored diagnosis at Rule E's
 * 500-character maximum, and free-text notes, always fit.
 */
@Entity
public class VisitRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(nullable = false, unique = true)
    private Appointment appointment;

    @Column(nullable = false, length = 1000)
    private String diagnosis;

    @Column(length = 4000)
    private String notes;

    @Column(length = 4000)
    private String prescription;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    protected VisitRecord() {
        // required by JPA
    }

    public VisitRecord(Appointment appointment, String diagnosis, String notes, String prescription,
                       LocalDateTime recordedAt) {
        this.appointment = appointment;
        this.diagnosis = diagnosis;
        this.notes = notes;
        this.prescription = prescription;
        this.recordedAt = recordedAt;
    }

    public Long getId() {
        return id;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public String getNotes() {
        return notes;
    }

    public String getPrescription() {
        return prescription;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }
}
