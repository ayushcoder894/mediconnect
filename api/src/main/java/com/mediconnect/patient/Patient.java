package com.mediconnect.patient;

import com.mediconnect.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;

@Entity
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    private String gender;

    @Column(name = "blood_group")
    private String bloodGroup;

    @Column(name = "height_cm")
    private BigDecimal heightCm;

    @Column(name = "weight_kg")
    private BigDecimal weightKg;

    private String address;

    @Column(name = "medical_history")
    private String medicalHistory;

    private String allergies;

    @Column(name = "smoking_history")
    private String smokingHistory;

    private boolean hypertension;

    @Column(name = "heart_disease")
    private boolean heartDisease;

    public Patient(User user) {
        this.user = user;
    }

    public Integer age() {
        return dateOfBirth == null ? null : Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    /** BMI from height and weight, or null if either is missing. */
    public BigDecimal bmi() {
        if (heightCm == null || weightKg == null || heightCm.signum() <= 0) {
            return null;
        }
        BigDecimal metres = heightCm.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return weightKg.divide(metres.multiply(metres), 1, RoundingMode.HALF_UP);
    }
}
