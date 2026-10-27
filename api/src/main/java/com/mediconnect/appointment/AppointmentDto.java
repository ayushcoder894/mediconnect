package com.mediconnect.appointment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AppointmentDto(Long id, Long doctorId, String doctorName, String specialization,
                             String hospitalName, String hospitalCity, Long patientId, String patientName,
                             String patientPhone, LocalDateTime slotStart, LocalDateTime slotEnd,
                             AppointmentStatus status, String reason, BigDecimal fee, String checkInCode,
                             LocalDateTime checkedInAt, boolean reviewed, boolean hasPrescription) {

    public static AppointmentDto from(Appointment a, boolean hasPrescription) {
        var d = a.getDoctor();
        var h = d.getHospital();
        var p = a.getPatient().getUser();
        return new AppointmentDto(a.getId(), d.getId(), d.getUser().getFullName(), d.getSpecialization(),
                h == null ? null : h.getName(), h == null ? null : h.getCity(), a.getPatient().getId(),
                p.getFullName(), p.getPhone(), a.getSlotStart(), a.getSlotEnd(), a.getStatus(), a.getReason(),
                a.getFee(), a.getCheckInCode(), a.getCheckedInAt(), a.isReviewed(), hasPrescription);
    }
}
