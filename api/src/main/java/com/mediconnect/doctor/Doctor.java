package com.mediconnect.doctor;

import com.mediconnect.hospital.Hospital;
import com.mediconnect.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;

    private String specialization;
    private String qualification;

    @Column(name = "experience_years")
    private int experienceYears;

    @Column(name = "consultation_fee")
    private BigDecimal consultationFee;

    private String bio;

    @Column(name = "slot_minutes")
    private int slotMinutes = 30;

    private boolean approved;

    @Column(name = "rating_avg")
    private BigDecimal ratingAvg = BigDecimal.ZERO;

    @Column(name = "rating_count")
    private int ratingCount;

    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayOfWeek")
    private List<DoctorAvailability> availability = new ArrayList<>();

    public void addRating(int rating) {
        BigDecimal total = ratingAvg.multiply(BigDecimal.valueOf(ratingCount)).add(BigDecimal.valueOf(rating));
        ratingCount++;
        ratingAvg = total.divide(BigDecimal.valueOf(ratingCount), 2, RoundingMode.HALF_UP);
    }
}
