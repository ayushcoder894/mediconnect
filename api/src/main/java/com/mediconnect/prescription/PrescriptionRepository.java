package com.mediconnect.prescription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    boolean existsByAppointmentId(Long appointmentId);

    @Query("select p.appointment.id from Prescription p where p.appointment.id in :ids")
    Set<Long> findAppointmentIdsWithPrescription(Collection<Long> ids);

    @Query("""
            select distinct p from Prescription p
            join fetch p.doctor d join fetch d.user left join fetch d.hospital
            join fetch p.patient pt join fetch pt.user join fetch p.appointment
            left join fetch p.items
            where pt.id = :patientId order by p.createdAt desc""")
    List<Prescription> findForPatient(Long patientId);

    @Query("""
            select distinct p from Prescription p
            join fetch p.doctor d join fetch d.user left join fetch d.hospital
            join fetch p.patient pt join fetch pt.user join fetch p.appointment
            left join fetch p.items
            where p.id = :id""")
    Optional<Prescription> findDetailed(Long id);

    @Query("""
            select distinct p from Prescription p
            join fetch p.doctor d join fetch d.user left join fetch d.hospital
            join fetch p.patient pt join fetch pt.user join fetch p.appointment
            left join fetch p.items
            where p.appointment.id = :appointmentId""")
    Optional<Prescription> findByAppointmentId(Long appointmentId);
}
