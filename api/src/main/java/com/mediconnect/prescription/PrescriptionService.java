package com.mediconnect.prescription;

import com.mediconnect.appointment.Appointment;
import com.mediconnect.appointment.AppointmentRepository;
import com.mediconnect.appointment.AppointmentStatus;
import com.mediconnect.audit.AuditService;
import com.mediconnect.common.ApiException;
import com.mediconnect.doctor.Doctor;
import com.mediconnect.doctor.DoctorService;
import com.mediconnect.notification.NotificationService;
import com.mediconnect.patient.PatientService;
import com.mediconnect.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptions;
    private final AppointmentRepository appointments;
    private final DoctorService doctorService;
    private final PatientService patientService;
    private final NotificationService notifications;
    private final AuditService audit;

    public record ItemDto(@NotBlank @Size(max = 120) String medicine,
                          @NotBlank @Size(max = 60) String dosage,
                          @NotBlank @Size(max = 60) String frequency,
                          @Min(1) @Max(365) int durationDays,
                          @Size(max = 255) String instructions) {
    }

    public record CreateRequest(@NotBlank @Size(max = 255) String diagnosis,
                                @Size(max = 2000) String notes,
                                @Future LocalDate followUpDate,
                                @NotEmpty @Size(max = 20) List<@Valid ItemDto> items) {
    }

    public record PrescriptionDto(Long id, Long appointmentId, LocalDateTime visitDate, String doctorName,
                                  String specialization, String hospitalName, Long patientId, String patientName,
                                  String diagnosis, String notes, LocalDate followUpDate,
                                  LocalDateTime createdAt, List<ItemDto> items) {

        public static PrescriptionDto from(Prescription p) {
            var d = p.getDoctor();
            return new PrescriptionDto(p.getId(), p.getAppointment().getId(), p.getAppointment().getSlotStart(),
                    d.getUser().getFullName(), d.getSpecialization(),
                    d.getHospital() == null ? null : d.getHospital().getName(), p.getPatient().getId(),
                    p.getPatient().getUser().getFullName(), p.getDiagnosis(), p.getNotes(), p.getFollowUpDate(),
                    p.getCreatedAt(), p.getItems().stream().map(i -> new ItemDto(i.getMedicine(), i.getDosage(),
                    i.getFrequency(), i.getDurationDays(), i.getInstructions())).toList());
        }
    }

    /** The doctor writes a prescription for their own appointment, which completes the visit. */
    @Transactional
    public PrescriptionDto create(Long appointmentId, CreateRequest req) {
        Doctor doctor = doctorService.currentDoctor();
        Appointment a = appointments.findDetailed(appointmentId)
                .filter(x -> x.getDoctor().getId().equals(doctor.getId()))
                .orElseThrow(() -> ApiException.notFound("Appointment"));
        if (!(a.getStatus() == AppointmentStatus.CONFIRMED || a.getStatus() == AppointmentStatus.CHECKED_IN
                || a.getStatus() == AppointmentStatus.COMPLETED || a.getStatus() == AppointmentStatus.BOOKED)) {
            throw ApiException.badRequest("Cannot prescribe for a " + a.getStatus().name().toLowerCase() + " appointment");
        }
        if (a.getSlotStart().toLocalDate().isAfter(LocalDate.now())) {
            throw ApiException.badRequest("You can write the prescription on or after the visit date");
        }
        if (prescriptions.existsByAppointmentId(appointmentId)) {
            throw ApiException.conflict("A prescription already exists for this appointment");
        }
        Prescription p = new Prescription();
        p.setAppointment(a);
        p.setDoctor(doctor);
        p.setPatient(a.getPatient());
        p.setDiagnosis(req.diagnosis().trim());
        p.setNotes(req.notes());
        p.setFollowUpDate(req.followUpDate());
        for (ItemDto i : req.items()) {
            PrescriptionItem item = new PrescriptionItem();
            item.setPrescription(p);
            item.setMedicine(i.medicine().trim());
            item.setDosage(i.dosage().trim());
            item.setFrequency(i.frequency().trim());
            item.setDurationDays(i.durationDays());
            item.setInstructions(i.instructions());
            p.getItems().add(item);
        }
        prescriptions.saveAndFlush(p);
        a.setStatus(AppointmentStatus.COMPLETED);
        notifications.enqueueEmail("rx:" + p.getId(), a.getPatient().getUser().getEmail(),
                "New prescription from " + doctor.getUser().getFullName(),
                "Your prescription for \"" + p.getDiagnosis() + "\" is ready. Log in to MediConnect to view or "
                        + "download it.\n\nMediConnect");
        audit.record("CREATE_PRESCRIPTION", "Prescription", p.getId(), "Appointment " + appointmentId);
        return PrescriptionDto.from(prescriptions.findDetailed(p.getId()).orElseThrow());
    }

    @Transactional(readOnly = true)
    public List<PrescriptionDto> mine() {
        var patient = patientService.currentPatient();
        return prescriptions.findForPatient(patient.getId()).stream().map(PrescriptionDto::from).toList();
    }

    @Transactional(readOnly = true)
    public PrescriptionDto byAppointment(Long appointmentId) {
        Prescription p = prescriptions.findByAppointmentId(appointmentId)
                .orElseThrow(() -> ApiException.notFound("Prescription"));
        assertCanView(p);
        return PrescriptionDto.from(p);
    }

    @Transactional(readOnly = true)
    public Prescription loadForCurrentUser(Long id) {
        Prescription p = prescriptions.findDetailed(id).orElseThrow(() -> ApiException.notFound("Prescription"));
        assertCanView(p);
        return p;
    }

    private void assertCanView(Prescription p) {
        var user = CurrentUser.get();
        boolean allowed = switch (user.role()) {
            case ADMIN -> true;
            case PATIENT -> p.getPatient().getUser().getId().equals(user.id());
            case DOCTOR -> appointments.existsByDoctorIdAndPatientId(
                    doctorService.currentDoctor().getId(), p.getPatient().getId());
        };
        if (!allowed) {
            throw ApiException.notFound("Prescription");
        }
    }
}
