package et.aau.clinic.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
}
