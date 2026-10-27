package com.mediconnect.lab;

import com.mediconnect.appointment.AppointmentRepository;
import com.mediconnect.audit.AuditService;
import com.mediconnect.common.ApiException;
import com.mediconnect.doctor.Doctor;
import com.mediconnect.doctor.DoctorService;
import com.mediconnect.lab.LabDtos.*;


import com.mediconnect.notification.NotificationService;
import com.mediconnect.patient.Patient;
import com.mediconnect.patient.PatientService;
import com.mediconnect.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LabService {

    private final LabTestRepository tests;
    private final LabBookingRepository bookings;
    private final AppointmentRepository appointments;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final NotificationService notifications;
    private final AuditService audit;

    // ---- catalog ----

    @Transactional(readOnly = true)
    public List<LabTestDto> catalog(String category, String q, boolean includeInactive) {
        String like = (q == null || q.isBlank()) ? null : "%" + q.trim().toLowerCase() + "%";
        String cat = (category == null || category.isBlank()) ? null : category;
        return tests.search(cat, like, includeInactive).stream().map(LabTestDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<String> categories() {
        return tests.findCategories();
    }

    @Transactional
    public LabTestDto createTest(UpsertLabTestRequest req) {
        if (tests.existsByCodeIgnoreCase(req.code())) {
            throw ApiException.conflict("A test with code " + req.code() + " already exists");
        }
        LabTest t = new LabTest();
        apply(t, req);
        tests.save(t);
        audit.record("CREATE_LAB_TEST", "LabTest", t.getId(), t.getName());
        return LabTestDto.from(t);
    }

    @Transactional
    public LabTestDto updateTest(Long id, UpsertLabTestRequest req) {
        LabTest t = tests.findById(id).orElseThrow(() -> ApiException.notFound("Lab test"));
        apply(t, req);
        audit.record("UPDATE_LAB_TEST", "LabTest", id, t.getName() + " price " + t.getPrice());
        return LabTestDto.from(t);
    }

    private void apply(LabTest t, UpsertLabTestRequest req) {
        t.setCode(req.code().trim().toUpperCase());
        t.setName(req.name().trim());
        t.setCategory(req.category().trim());
        t.setDescription(req.description());
        t.setPrice(req.price());
        t.setPartnerPrice(req.partnerPrice());
        t.setSampleType(req.sampleType());
        t.setTurnaroundHours(req.turnaroundHours());
        t.setParameters(req.parameters() == null || req.parameters().isBlank() ? null : req.parameters().trim());
        t.setActive(req.active());
    }

    // ---- patient ----

    @Transactional
    public LabBookingDto book(BookLabRequest req) {
        Patient patient = patientService.currentPatient();
        LabTest test = tests.findById(req.testId()).filter(LabTest::isActive)
                .orElseThrow(() -> ApiException.notFound("Lab test"));
        LabBooking b = create(patient, test, req.scheduledAt(), null);
        audit.record("BOOK_LAB_TEST", "LabBooking", b.getId(), test.getName());
        return LabBookingDto.from(b);
    }

    @Transactional(readOnly = true)
    public List<LabBookingDto> mine() {
        Patient patient = patientService.currentPatient();
        return bookings.findForPatient(patient.getId()).stream().map(LabBookingDto::from).toList();
    }

    @Transactional
    public LabBookingDto cancel(Long id) {
        Patient patient = patientService.currentPatient();
        LabBooking b = bookings.findDetailed(id)
                .filter(x -> x.getPatient().getId().equals(patient.getId()))
                .orElseThrow(() -> ApiException.notFound("Lab booking"));
        if (b.getStatus() != LabBooking.Status.BOOKED) {
            throw ApiException.badRequest("Only bookings whose sample is not yet collected can be cancelled");
        }
        b.setStatus(LabBooking.Status.CANCELLED);
        return LabBookingDto.from(b);
    }

    // ---- doctor ----

    /** A doctor orders a test for a patient they have an appointment with. */
    @Transactional
    public LabBookingDto orderByDoctor(DoctorOrderRequest req) {
        Doctor doctor = doctorService.currentDoctor();
        var appt = appointments.findDetailed(req.appointmentId())
                .filter(a -> a.getDoctor().getId().equals(doctor.getId()))
                .orElseThrow(() -> ApiException.notFound("Appointment"));
        LabTest test = tests.findById(req.testId()).filter(LabTest::isActive)
                .orElseThrow(() -> ApiException.notFound("Lab test"));
        LabBooking b = create(appt.getPatient(), test, LocalDateTime.now().plusDays(1).withHour(9).withMinute(0)
                .withSecond(0).withNano(0), doctor);
        audit.record("ORDER_LAB_TEST", "LabBooking", b.getId(),
                test.getName() + " for patient " + appt.getPatient().getId());
        return LabBookingDto.from(b);
    }

    private LabBooking create(Patient patient, LabTest test, LocalDateTime when, Doctor orderedBy) {
        LabBooking b = new LabBooking();
        b.setPatient(patient);
        b.setLabTest(test);
        b.setScheduledAt(when);
        b.setPrice(test.getPrice());
        b.setOrderedBy(orderedBy);
        bookings.save(b);
        notifications.enqueueEmail("lab-booked:" + b.getId(), patient.getUser().getEmail(),
                "Lab test booked: " + test.getName(),
                "Your " + test.getName() + " is scheduled for " + when.toLocalDate() + ". Sample: "
                        + test.getSampleType() + ".\n\nMediConnect");
        return b;
    }

    // ---- lab staff / admin ----

    @Transactional(readOnly = true)
    public List<LabBookingDto> forAdmin(LabBooking.Status status) {
        return bookings.findForAdmin(status, PageRequest.of(0, 200)).stream().map(LabBookingDto::from).toList();
    }

    @Transactional
    public LabBookingDto updateStatus(Long id, LabBooking.Status status) {
        LabBooking b = bookings.findDetailed(id).orElseThrow(() -> ApiException.notFound("Lab booking"));
        if (status == LabBooking.Status.REPORT_READY) {
            throw ApiException.badRequest("Submit the results to mark the report ready");
        }
        b.setStatus(status);
        audit.record("LAB_STATUS", "LabBooking", id, status.name());
        return LabBookingDto.from(b);
    }

    /**
     * Stores results (replacing earlier ones), flags each numeric value against its
     * reference range, marks the report ready and notifies the patient.
     */
    @Transactional
    public LabBookingDto submitResults(Long id, Map<String, String> values) {
        LabBooking b = bookings.findDetailed(id).orElseThrow(() -> ApiException.notFound("Lab booking"));
        if (b.getStatus() == LabBooking.Status.CANCELLED) {
            throw ApiException.badRequest("This booking was cancelled");
        }
        var template = b.getLabTest().parameterList();
        b.getResults().clear();
        if (template.isEmpty()) {
            values.forEach((k, v) -> b.getResults().add(result(b, k, v, null, null)));
        } else {
            for (LabTest.Parameter p : template) {
                String v = values.get(p.name());
                if (v == null || v.isBlank()) {
                    throw ApiException.badRequest("Missing value for " + p.name());
                }
                b.getResults().add(result(b, p.name(), v.trim(), p.unit(), p.referenceRange()));
            }
        }
        b.setStatus(LabBooking.Status.REPORT_READY);
        notifications.enqueueEmail("lab-ready:" + b.getId(), b.getPatient().getUser().getEmail(),
                "Your " + b.getLabTest().getName() + " report is ready",
                "Your lab report is ready. Log in to MediConnect to view and download it.\n\nMediConnect");
        audit.record("LAB_RESULTS", "LabBooking", id, b.getLabTest().getName());
        return LabBookingDto.from(b);
    }

    private LabResult result(LabBooking b, String parameter, String value, String unit, String range) {
        LabResult r = new LabResult();
        r.setBooking(b);
        r.setParameter(parameter);
        r.setValue(value);
        r.setUnit(unit);
        r.setReferenceRange(range);
        r.setFlag(LabResult.flagFor(value, range));
        return r;
    }

    /** Booking for a report download, checking that the caller may see it. */
    @Transactional(readOnly = true)
    public LabBooking loadForReport(Long id) {
        LabBooking b = bookings.findDetailed(id).orElseThrow(() -> ApiException.notFound("Lab booking"));
        var user = CurrentUser.get();
        boolean allowed = switch (user.role()) {
            case ADMIN -> true;
            case PATIENT -> b.getPatient().getUser().getId().equals(user.id());
            case DOCTOR -> appointments.existsByDoctorIdAndPatientId(
                    doctorService.currentDoctor().getId(), b.getPatient().getId());
        };
        if (!allowed) {
            throw ApiException.notFound("Lab booking");
        }
        if (b.getStatus() != LabBooking.Status.REPORT_READY) {
            throw ApiException.badRequest("The report is not ready yet");
        }
        b.getResults().size();
        return b;
    }
}
