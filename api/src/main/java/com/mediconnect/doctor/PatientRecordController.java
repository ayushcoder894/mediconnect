package com.mediconnect.doctor;

import com.mediconnect.appointment.AppointmentDto;
import com.mediconnect.appointment.AppointmentRepository;
import com.mediconnect.audit.AuditService;
import com.mediconnect.common.ApiException;
import com.mediconnect.lab.LabBookingRepository;
import com.mediconnect.lab.LabDtos;
import com.mediconnect.patient.PatientRepository;
import com.mediconnect.patient.PatientService;
import com.mediconnect.prescription.PrescriptionRepository;
import com.mediconnect.prescription.PrescriptionService;
import com.mediconnect.risk.RiskAssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A doctor's view of a patient's medical record. Only doctors who have had an
 * appointment with the patient can open it, and every access is audit-logged.
 */
@RestController
@PreAuthorize("hasRole('DOCTOR')")
@RequiredArgsConstructor
public class PatientRecordController {

    private final DoctorService doctorService;
    private final PatientRepository patients;
    private final AppointmentRepository appointments;
    private final LabBookingRepository labBookings;
    private final PrescriptionRepository prescriptions;
    private final RiskAssessmentRepository risks;
    private final AuditService audit;

    public record RiskPoint(LocalDateTime createdAt, BigDecimal probability, String riskBand, BigDecimal hba1c,
                            int glucose) {
    }

    public record PatientRecord(PatientService.PatientDto patient, List<AppointmentDto> appointments,
                                List<LabDtos.LabBookingDto> labBookings,
                                List<PrescriptionService.PrescriptionDto> prescriptions,
                                List<RiskPoint> riskAssessments) {
    }

    @GetMapping("/api/doctor/patients/{patientId}")
    @Transactional(readOnly = true)
    public PatientRecord record(@PathVariable Long patientId) {
        Doctor doctor = doctorService.currentDoctor();
        if (!appointments.existsByDoctorIdAndPatientId(doctor.getId(), patientId)) {
            throw ApiException.forbidden("You can only view records of your own patients");
        }
        var patient = patients.findById(patientId).orElseThrow(() -> ApiException.notFound("Patient"));
        audit.record("VIEW_PATIENT_RECORD", "Patient", patientId, null);
        return new PatientRecord(
                PatientService.PatientDto.from(patient),
                appointments.findForPatient(patientId).stream().map(a -> AppointmentDto.from(a, false)).toList(),
                labBookings.findForPatient(patientId).stream().map(LabDtos.LabBookingDto::from).toList(),
                prescriptions.findForPatient(patientId).stream().map(PrescriptionService.PrescriptionDto::from).toList(),
                risks.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                        .map(r -> new RiskPoint(r.getCreatedAt(), r.getProbability(), r.getRiskBand(), r.getHba1c(),
                                r.getGlucose()))
                        .toList());
    }
}
