package com.mediconnect.appointment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("""
            select a.slotStart from Appointment a
            where a.doctor.id = :doctorId and a.status <> com.mediconnect.appointment.AppointmentStatus.CANCELLED
              and a.slotStart >= :from and a.slotStart < :to""")
    List<LocalDateTime> findTakenSlots(Long doctorId, LocalDateTime from, LocalDateTime to);

    @Query("""
            select a from Appointment a
            join fetch a.doctor d join fetch d.user left join fetch d.hospital
            join fetch a.patient p join fetch p.user
            where p.id = :patientId order by a.slotStart desc""")
    List<Appointment> findForPatient(Long patientId);

    @Query("""
            select a from Appointment a
            join fetch a.doctor d join fetch d.user left join fetch d.hospital
            join fetch a.patient p join fetch p.user
            where d.id = :doctorId and a.slotStart >= :from and a.slotStart < :to
            order by a.slotStart""")
    List<Appointment> findForDoctor(Long doctorId, LocalDateTime from, LocalDateTime to);

    @Query("""
            select a from Appointment a
            join fetch a.doctor d join fetch d.user left join fetch d.hospital
            join fetch a.patient p join fetch p.user
            where a.id = :id""")
    Optional<Appointment> findDetailed(Long id);

    @Query("""
            select a from Appointment a
            join fetch a.doctor d join fetch d.user left join fetch d.hospital
            join fetch a.patient p join fetch p.user
            where a.checkInCode = :code""")
    Optional<Appointment> findByCheckInCode(String code);

    @Query("""
            select a from Appointment a
            join fetch a.doctor d join fetch d.user
            join fetch a.patient p join fetch p.user
            where a.status in :statuses and a.slotStart >= :from and a.slotStart < :to""")
    List<Appointment> findByStatusInAndSlotBetween(Collection<AppointmentStatus> statuses,
                                                   LocalDateTime from, LocalDateTime to);

    @Query("""
            select count(a) > 0 from Appointment a
            where a.patient.id = :patientId and a.status <> com.mediconnect.appointment.AppointmentStatus.CANCELLED
              and a.slotStart < :end and a.slotEnd > :start""")
    boolean patientHasOverlap(Long patientId, LocalDateTime start, LocalDateTime end);

    boolean existsByDoctorIdAndPatientId(Long doctorId, Long patientId);
}
