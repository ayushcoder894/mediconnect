package com.mediconnect.appointment;

import com.mediconnect.doctor.Doctor;
import com.mediconnect.patient.Patient;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @Column(name = "slot_start")
    private LocalDateTime slotStart;

    @Column(name = "slot_end")
    private LocalDateTime slotEnd;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status = AppointmentStatus.BOOKED;

    private String reason;

    private BigDecimal fee;

    @Column(name = "check_in_code")
    private String checkInCode = UUID.randomUUID().toString();

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    private boolean reviewed;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    private int version;

    public Appointment(Doctor doctor, Patient patient, LocalDateTime slotStart, String reason) {
        this.doctor = doctor;
        this.patient = patient;
        this.slotStart = slotStart;
        this.slotEnd = slotStart.plusMinutes(doctor.getSlotMinutes());
        this.reason = reason;
        this.fee = doctor.getConsultationFee();
    }
}
