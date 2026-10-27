package com.mediconnect.lab;

import com.mediconnect.doctor.Doctor;
import com.mediconnect.patient.Patient;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lab_bookings")
@Getter
@Setter
@NoArgsConstructor
public class LabBooking {

    public enum Status { BOOKED, SAMPLE_COLLECTED, REPORT_READY, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lab_test_id")
    private LabTest labTest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordered_by_doctor_id")
    private Doctor orderedBy;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    private Status status = Status.BOOKED;

    private BigDecimal price;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<LabResult> results = new ArrayList<>();
}
