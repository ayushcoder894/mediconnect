package com.mediconnect.lab;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LabBookingRepository extends JpaRepository<LabBooking, Long> {

    @Query("""
            select b from LabBooking b join fetch b.labTest join fetch b.patient p join fetch p.user
            left join fetch b.orderedBy o left join fetch o.user
            where p.id = :patientId order by b.scheduledAt desc""")
    List<LabBooking> findForPatient(Long patientId);

    @Query("""
            select b from LabBooking b join fetch b.labTest join fetch b.patient p join fetch p.user
            left join fetch b.orderedBy o left join fetch o.user
            where (:status is null or b.status = :status)
            order by b.scheduledAt desc""")
    List<LabBooking> findForAdmin(LabBooking.Status status, Pageable pageable);

    @Query("""
            select b from LabBooking b join fetch b.labTest join fetch b.patient p join fetch p.user
            left join fetch b.orderedBy o left join fetch o.user
            where b.id = :id""")
    Optional<LabBooking> findDetailed(Long id);
}
