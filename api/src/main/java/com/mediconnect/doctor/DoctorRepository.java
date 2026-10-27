package com.mediconnect.doctor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    @Query("""
            select d from Doctor d join fetch d.user u left join fetch d.hospital h
            where d.approved = true and u.enabled = true
              and (:specialization is null or d.specialization = :specialization)
              and (:q is null or lower(u.fullName) like :q or lower(d.specialization) like :q)
              and (:city is null or h.city = :city)
              and (:maxFee is null or d.consultationFee <= :maxFee)
            order by d.ratingAvg desc, d.experienceYears desc""")
    List<Doctor> search(String specialization, String q, String city, BigDecimal maxFee);

    @Query("select d from Doctor d join fetch d.user left join fetch d.hospital where d.id = :id")
    Optional<Doctor> findWithUser(Long id);

    @Query("select d from Doctor d join fetch d.user left join fetch d.hospital where d.user.id = :userId")
    Optional<Doctor> findByUserId(Long userId);

    @Query("select distinct d.specialization from Doctor d where d.approved = true order by d.specialization")
    List<String> findSpecializations();

    @Query("select d from Doctor d join fetch d.user left join fetch d.hospital order by d.approved, d.id desc")
    List<Doctor> findAllForAdmin();

    long countByApprovedTrue();
}
