package com.mediconnect.prescription;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "prescription_items")
@Getter
@Setter
@NoArgsConstructor
public class PrescriptionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    private String medicine;
    private String dosage;
    private String frequency;

    @Column(name = "duration_days")
    private int durationDays;

    private String instructions;
}
