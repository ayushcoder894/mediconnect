package com.mediconnect.lab;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LabResultRepository extends JpaRepository<LabResult, Long> {

    @Query("""
            select r from LabResult r join fetch r.booking b
            where b.patient.id = :patientId and r.parameter = :parameter
            order by b.scheduledAt desc""")
    List<LabResult> findLatest(Long patientId, String parameter, Pageable pageable);
}
