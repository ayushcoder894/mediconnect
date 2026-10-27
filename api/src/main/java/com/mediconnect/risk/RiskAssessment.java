package com.mediconnect.risk;

import com.mediconnect.patient.Patient;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "risk_assessments")
@Getter
@Setter
@NoArgsConstructor
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    private String gender;
    private int age;
    private boolean hypertension;

    @Column(name = "heart_disease")
    private boolean heartDisease;

    @Column(name = "smoking_history")
    private String smokingHistory;

    private BigDecimal bmi;
    private BigDecimal hba1c;
    private int glucose;
    private BigDecimal probability;

    @Column(name = "risk_band")
    private String riskBand;

    @Column(name = "model_version")
    private String modelVersion;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
