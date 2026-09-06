package et.aau.clinic.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A patient's booking of a slot. feeCategory and feeAmount are captured
 * at booking time (Rule 1) rather than recomputed later, so a patient's
 * birthday or a future change to the fee bands never rewrites history.
 * cancellationFee stays null unless Rule 3b's late-cancellation guard
 * actually fired.
 */
@Entity
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Patient patient;

    @ManyToOne(optional = false)
    private Slot slot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeeCategory feeCategory;

    @Column(nullable = false)
    private BigDecimal feeAmount;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private BigDecimal cancellationFee;

    /**
     * When the 24-hour reminder was sent (hospital-expansion Rule F).
     * Stays null until ReminderPolicy fires; its non-null-ness is also
     * condition C2 ("not already reminded"), so the scheduled job never
     * sends twice.
     */
    private LocalDateTime reminderSentAt;

    /**
     * What the patient actually pays after their insurance coverage is
     * applied to feeAmount (hospital-expansion Rule G). Captured at
     * booking time like feeAmount, so a later change to the patient's
     * coverage never rewrites an existing bill. Equals feeAmount when
     * the patient has no coverage.
     */
    private BigDecimal netPayable;

    protected Appointment() {
        // required by JPA
    }

    public Appointment(Patient patient, Slot slot, AppointmentStatus status, FeeCategory feeCategory,
                        BigDecimal feeAmount, LocalDateTime requestedAt) {
        this.patient = patient;
        this.slot = slot;
        this.status = status;
        this.feeCategory = feeCategory;
        this.feeAmount = feeAmount;
        this.requestedAt = requestedAt;
    }

    public Long getId() {
        return id;
    }

    public Patient getPatient() {
        return patient;
    }

    public Slot getSlot() {
        return slot;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public FeeCategory getFeeCategory() {
        return feeCategory;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public BigDecimal getCancellationFee() {
        return cancellationFee;
    }

    public void setCancellationFee(BigDecimal cancellationFee) {
        this.cancellationFee = cancellationFee;
    }

    public LocalDateTime getReminderSentAt() {
        return reminderSentAt;
    }

    public void setReminderSentAt(LocalDateTime reminderSentAt) {
        this.reminderSentAt = reminderSentAt;
    }

    public BigDecimal getNetPayable() {
        return netPayable;
    }

    public void setNetPayable(BigDecimal netPayable) {
        this.netPayable = netPayable;
    }
}
