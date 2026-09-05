package et.aau.clinic.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;

/**
 * A doctor, belonging to one department. specialty is a plain String
 * rather than an enum - EXPANSION.md does not define a closed list of
 * specialties, and inventing one would be scope nobody asked for. If a
 * closed list is wanted later, that becomes a real EP-style validation
 * rule in core/, not a guess made here.
 */
@Entity
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String specialty;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private Department department;

    /**
     * The doctor's photo as a data URL ("data:image/jpeg;base64,...") uploaded from the
     * admin's device. Stored inline rather than as a file on disk: H2 here is in-memory
     * and thrown away on restart, so there is nothing a filesystem copy would add.
     * Nullable - a doctor without a photo just renders a placeholder.
     */
    @Lob
    @Column
    private String photo;

    /**
     * The most patients this doctor will see in a single day (hospital-expansion:
     * doctor capacity). Feeds Rule G / DoctorLoad. Defaults to 8; reception can
     * change it per doctor.
     */
    @Column(nullable = false)
    private int dailyPatientLimit = DEFAULT_DAILY_PATIENT_LIMIT;

    public static final int DEFAULT_DAILY_PATIENT_LIMIT = 8;

    protected Doctor() {
        // required by JPA
    }

    public Doctor(String name, String specialty, Department department) {
        this.name = name;
        this.specialty = specialty;
        this.department = department;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public int getDailyPatientLimit() {
        return dailyPatientLimit;
    }

    public void setDailyPatientLimit(int dailyPatientLimit) {
        this.dailyPatientLimit = dailyPatientLimit;
    }
}
