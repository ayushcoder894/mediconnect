package com.mediconnect.patient;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    @Query("select p from Patient p join fetch p.user where p.user.id = :userId")
    Optional<Patient> findByUserId(Long userId);
}
