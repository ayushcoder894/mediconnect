package com.mediconnect.appointment;

import com.mediconnect.audit.AuditService;
import com.mediconnect.common.ApiException;
import com.mediconnect.doctor.Doctor;
import com.mediconnect.doctor.DoctorRepository;
import com.mediconnect.doctor.DoctorService;
import com.mediconnect.notification.NotificationService;
import com.mediconnect.patient.Patient;
import com.mediconnect.patient.PatientService;
import com.mediconnect.prescription.PrescriptionRepository;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEE, d MMM yyyy 'at' h:mm a");

    private final AppointmentRepository appointments;
    private final DoctorRepository doctors;
    private final DoctorService doctorService;
    private final PatientService patientService;
    private final PrescriptionRepository prescriptions;
    private final NotificationService notifications;
    private final AuditService audit;

    public record BookRequest(@NotNull Long doctorId, @NotNull @Future LocalDateTime slotStart,
                              @Size(max = 500) String reason) {
    }

    public record RescheduleRequest(@NotNull @Future LocalDateTime slotStart) {
    }

    public record StatusRequest(@NotNull AppointmentStatus status) {
    }

    /**
     * Books a slot. Two patients racing for the same slot both pass the checks below,
     * but only one insert can satisfy the UNIQUE (doctor_id, active_slot) constraint;
     * the loser gets a clear 409 instead of a double-booking.
     */
    @Transactional
    public AppointmentDto book(BookRequest req) {
        Patient patient = patientService.currentPatient();
        Appointment saved = reserve(patient, req.doctorId(), req.slotStart(), req.reason());
        notifications.enqueueEmail("appt-booked:" + saved.getId(), patient.getUser().getEmail(),
                "Appointment confirmed with " + saved.getDoctor().getUser().getFullName(),
                "Hi " + patient.getUser().getFullName() + ",\n\nYour appointment with "
                        + saved.getDoctor().getUser().getFullName() + " (" + saved.getDoctor().getSpecialization()
                        + ") is booked for " + saved.getSlotStart().format(WHEN) + ".\n\n"
                        + "Show the QR code on your appointment slip at the front desk to check in.\n\nMediConnect");
        audit.record("BOOK_APPOINTMENT", "Appointment", saved.getId(),
                "Doctor " + req.doctorId() + " at " + req.slotStart());
        return AppointmentDto.from(saved, false);
    }

    private Appointment reserve(Patient patient, Long doctorId, LocalDateTime slotStart, String reason) {
        Doctor doctor = doctors.findById(doctorId).filter(Doctor::isApproved)
                .orElseThrow(() -> ApiException.notFound("Doctor"));
        if (!slotStart.isAfter(LocalDateTime.now())) {
            throw ApiException.badRequest("Please choose a time in the future");
        }
        if (!DoctorService.isValidSlot(doctor, slotStart)) {
            throw ApiException.badRequest("The doctor is not available at that time");
        }
        LocalDateTime end = slotStart.plusMinutes(doctor.getSlotMinutes());
        if (appointments.patientHasOverlap(patient.getId(), slotStart, end)) {
            throw ApiException.conflict("You already have an appointment at that time");
        }
        try {
            return appointments.saveAndFlush(new Appointment(doctor, patient, slotStart, reason));
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict("Sorry, that slot was just booked by someone else. Please pick another time.");
        }
    }

    @Transactional(readOnly = true)
    public List<AppointmentDto> mine() {
        Patient patient = patientService.currentPatient();
        return toDtos(appointments.findForPatient(patient.getId()));
    }

    @Transactional
    public AppointmentDto cancel(Long id) {
        Appointment a = ownedByCurrentPatient(id);
        if (!(a.getStatus() == AppointmentStatus.BOOKED || a.getStatus() == AppointmentStatus.CONFIRMED)
                || !a.getSlotStart().isAfter(LocalDateTime.now())) {
            throw ApiException.badRequest("Only upcoming appointments can be cancelled");
        }
        a.setStatus(AppointmentStatus.CANCELLED);
        notifications.enqueueEmail("appt-cancelled:" + a.getId(), a.getPatient().getUser().getEmail(),
                "Appointment cancelled",
                "Your appointment with " + a.getDoctor().getUser().getFullName() + " on "
                        + a.getSlotStart().format(WHEN) + " has been cancelled.\n\nMediConnect");
        audit.record("CANCEL_APPOINTMENT", "Appointment", id, null);
        return AppointmentDto.from(a, false);
    }

    /**
     * Cancels the old slot and books the new one in a single transaction: if the new
     * slot is taken, everything rolls back and the original appointment is kept.
     */
    @Transactional
    public AppointmentDto reschedule(Long id, RescheduleRequest req) {
        Appointment old = ownedByCurrentPatient(id);
        if (!(old.getStatus() == AppointmentStatus.BOOKED || old.getStatus() == AppointmentStatus.CONFIRMED)) {
            throw ApiException.badRequest("This appointment can no longer be rescheduled");
        }
        old.setStatus(AppointmentStatus.CANCELLED);
        appointments.flush();
        Appointment fresh = reserve(old.getPatient(), old.getDoctor().getId(), req.slotStart(), old.getReason());
        notifications.enqueueEmail("appt-rescheduled:" + fresh.getId(), old.getPatient().getUser().getEmail(),
                "Appointment rescheduled",
                "Your appointment with " + fresh.getDoctor().getUser().getFullName() + " has moved to "
                        + fresh.getSlotStart().format(WHEN) + ".\n\nMediConnect");
        audit.record("RESCHEDULE_APPOINTMENT", "Appointment", fresh.getId(), "Replaces " + id);
        return AppointmentDto.from(fresh, false);
    }

    @Transactional(readOnly = true)
    public AppointmentDto getForCurrentUser(Long id) {
        Appointment a = appointments.findDetailed(id).orElseThrow(() -> ApiException.notFound("Appointment"));
        assertCanView(a);
        return AppointmentDto.from(a, prescriptions.existsByAppointmentId(id));
    }

    public Appointment loadForCurrentUser(Long id) {
        Appointment a = appointments.findDetailed(id).orElseThrow(() -> ApiException.notFound("Appointment"));
        assertCanView(a);
        return a;
    }

    // ---- doctor side ----

    @Transactional(readOnly = true)
    public List<AppointmentDto> forDoctor(LocalDate from, LocalDate to) {
        Doctor doctor = doctorService.currentDoctor();
        return toDtos(appointments.findForDoctor(doctor.getId(), from.atStartOfDay(), to.plusDays(1).atStartOfDay()));
    }

    @Transactional
    public AppointmentDto updateStatusByDoctor(Long id, AppointmentStatus next) {
        Doctor doctor = doctorService.currentDoctor();
        Appointment a = appointments.findDetailed(id).orElseThrow(() -> ApiException.notFound("Appointment"));
        if (!a.getDoctor().getId().equals(doctor.getId())) {
            throw ApiException.forbidden("This is not your appointment");
        }
        if (!a.getStatus().canMoveTo(next)) {
            throw ApiException.badRequest("Cannot change status from " + a.getStatus() + " to " + next);
        }
        if (next == AppointmentStatus.NO_SHOW && a.getSlotStart().isAfter(LocalDateTime.now())) {
            throw ApiException.badRequest("An appointment can only be marked no-show after it starts");
        }
        if (next == AppointmentStatus.COMPLETED && a.getSlotStart().toLocalDate().isAfter(LocalDate.now())) {
            throw ApiException.badRequest("A visit can only be completed on or after its date");
        }
        a.setStatus(next);
        if (next == AppointmentStatus.CANCELLED) {
            notifications.enqueueEmail("appt-cancelled:" + a.getId(), a.getPatient().getUser().getEmail(),
                    "Appointment cancelled by the doctor",
                    "Unfortunately " + a.getDoctor().getUser().getFullName() + " had to cancel your appointment on "
                            + a.getSlotStart().format(WHEN) + ". Please book another slot.\n\nMediConnect");
        }
        audit.record("APPOINTMENT_STATUS", "Appointment", id, next.name());
        return AppointmentDto.from(a, prescriptions.existsByAppointmentId(id));
    }

    // ---- front desk ----

    @Transactional
    public AppointmentDto checkIn(String code) {
        Appointment a = appointments.findByCheckInCode(code.trim())
                .orElseThrow(() -> ApiException.notFound("Appointment for this QR code"));
        if (a.getStatus() == AppointmentStatus.CHECKED_IN) {
            throw ApiException.conflict(a.getPatient().getUser().getFullName() + " is already checked in");
        }
        if (!a.getStatus().canMoveTo(AppointmentStatus.CHECKED_IN)) {
            throw ApiException.badRequest("This appointment is " + a.getStatus().name().toLowerCase());
        }
        if (!a.getSlotStart().toLocalDate().equals(LocalDate.now())) {
            throw ApiException.badRequest("This appointment is on " + a.getSlotStart().toLocalDate() + ", not today");
        }
        a.setStatus(AppointmentStatus.CHECKED_IN);
        a.setCheckedInAt(LocalDateTime.now());
        audit.record("CHECK_IN", "Appointment", a.getId(), a.getPatient().getUser().getFullName());
        return AppointmentDto.from(a, false);
    }

    // ---- helpers ----

    private Appointment ownedByCurrentPatient(Long id) {
        Patient patient = patientService.currentPatient();
        Appointment a = appointments.findDetailed(id).orElseThrow(() -> ApiException.notFound("Appointment"));
        if (!a.getPatient().getId().equals(patient.getId())) {
            throw ApiException.notFound("Appointment");
        }
        return a;
    }

    private void assertCanView(Appointment a) {
        var user = com.mediconnect.security.CurrentUser.get();
        boolean allowed = switch (user.role()) {
            case ADMIN -> true;
            case PATIENT -> a.getPatient().getUser().getId().equals(user.id());
            case DOCTOR -> a.getDoctor().getUser().getId().equals(user.id());
        };
        if (!allowed) {
            throw ApiException.notFound("Appointment");
        }
    }

    private List<AppointmentDto> toDtos(List<Appointment> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Set<Long> withRx = prescriptions.findAppointmentIdsWithPrescription(
                list.stream().map(Appointment::getId).toList());
        return list.stream().map(a -> AppointmentDto.from(a, withRx.contains(a.getId()))).toList();
    }
}
