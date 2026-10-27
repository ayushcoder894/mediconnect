package com.mediconnect.risk;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, Long> {

    List<RiskAssessment> findByPatientIdOrderByCreatedAtDesc(Long patientId);
}
