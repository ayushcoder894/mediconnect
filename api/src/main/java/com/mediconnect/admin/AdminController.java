package com.mediconnect.admin;

import com.mediconnect.appointment.AppointmentDto;
import com.mediconnect.appointment.AppointmentService;
import com.mediconnect.audit.AuditLog;
import com.mediconnect.audit.AuditService;
import com.mediconnect.common.ApiException;
import com.mediconnect.doctor.Doctor;
import com.mediconnect.doctor.DoctorRepository;
import com.mediconnect.doctor.DoctorService;
import com.mediconnect.hospital.HospitalService;
import com.mediconnect.lab.LabBooking;
import com.mediconnect.lab.LabDtos;
import com.mediconnect.lab.LabService;
import com.mediconnect.notification.NotificationLog;
import com.mediconnect.notification.NotificationLogRepository;
import com.mediconnect.notification.OutboxMessage;
import com.mediconnect.notification.OutboxRepository;
import com.mediconnect.security.CurrentUser;
import com.mediconnect.user.Role;
import com.mediconnect.user.User;
import com.mediconnect.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/** Admin-only endpoints. Access is enforced for the whole /api/admin/** path in SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AnalyticsService analytics;
    private final DoctorRepository doctors;
    private final UserRepository users;
    private final LabService labService;
    private final HospitalService hospitalService;
    private final AppointmentService appointmentService;
    private final AuditService audit;
    private final OutboxRepository outbox;
    private final NotificationLogRepository notificationLogs;

    public record UserDto(Long id, String fullName, String email, String phone, Role role, boolean enabled,
                          LocalDateTime createdAt) {
    }

    public record EnabledRequest(boolean enabled) {
    }

    public record QuantityRequest(@Min(0) @Max(100000) int quantity) {
    }

    public record CheckInRequest(@NotBlank String code) {
    }

    public record OutboxDto(Long id, String recipient, String subject, OutboxMessage.Status status, int attempts,
                            LocalDateTime nextAttemptAt, LocalDateTime createdAt, LocalDateTime sentAt,
                            String lastError) {
    }

    @GetMapping("/dashboard")
    public AnalyticsService.Dashboard dashboard() {
        return analytics.dashboard();
    }

    // ---- doctors and users ----

    @GetMapping("/doctors")
    @Transactional(readOnly = true)
    public List<DoctorService.DoctorSummary> allDoctors() {
        return doctors.findAllForAdmin().stream().map(DoctorService.DoctorSummary::from).toList();
    }

    @PostMapping("/doctors/{id}/approve")
    @Transactional
    public DoctorService.DoctorSummary approve(@PathVariable Long id, @RequestBody EnabledRequest req) {
        Doctor d = doctors.findWithUser(id).orElseThrow(() -> ApiException.notFound("Doctor"));
        d.setApproved(req.enabled());
        audit.record(req.enabled() ? "APPROVE_DOCTOR" : "SUSPEND_DOCTOR", "Doctor", id, d.getUser().getEmail());
        return DoctorService.DoctorSummary.from(d);
    }

    @GetMapping("/users")
    public List<UserDto> users() {
        return users.findAllByOrderByIdDesc().stream()
                .map(u -> new UserDto(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.getRole(),
                        u.isEnabled(), u.getCreatedAt()))
                .toList();
    }

    @PatchMapping("/users/{id}/enabled")
    @Transactional
    public void setEnabled(@PathVariable Long id, @RequestBody EnabledRequest req) {
        if (id.equals(CurrentUser.get().id())) {
            throw ApiException.badRequest("You cannot disable your own account");
        }
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("User"));
        u.setEnabled(req.enabled());
        audit.record(req.enabled() ? "ENABLE_USER" : "DISABLE_USER", "User", id, u.getEmail());
    }

    // ---- lab ----

    @GetMapping("/lab-tests")
    public List<LabDtos.LabTestDto> labTests() {
        return labService.catalog(null, null, true);
    }

    @PostMapping("/lab-tests")
    public LabDtos.LabTestDto createLabTest(@Valid @RequestBody LabDtos.UpsertLabTestRequest req) {
        return labService.createTest(req);
    }

    @PutMapping("/lab-tests/{id}")
    public LabDtos.LabTestDto updateLabTest(@PathVariable Long id, @Valid @RequestBody LabDtos.UpsertLabTestRequest req) {
        return labService.updateTest(id, req);
    }

    @GetMapping("/lab-bookings")
    public List<LabDtos.LabBookingDto> labBookings(@RequestParam(required = false) LabBooking.Status status) {
        return labService.forAdmin(status);
    }

    @PatchMapping("/lab-bookings/{id}/status")
    public LabDtos.LabBookingDto labStatus(@PathVariable Long id, @Valid @RequestBody LabDtos.StatusRequest req) {
        return labService.updateStatus(id, req.status());
    }

    @PostMapping("/lab-bookings/{id}/results")
    public LabDtos.LabBookingDto labResults(@PathVariable Long id, @Valid @RequestBody LabDtos.ResultsRequest req) {
        return labService.submitResults(id, req.values());
    }

    // ---- stock ----

    @PutMapping("/blood/{id}")
    public void updateBlood(@PathVariable Long id, @Valid @RequestBody QuantityRequest req) {
        hospitalService.updateBlood(id, req.quantity());
    }

    @PutMapping("/organs/{id}")
    public void updateOrgan(@PathVariable Long id, @Valid @RequestBody QuantityRequest req) {
        hospitalService.updateOrgan(id, req.quantity());
    }

    // ---- front desk ----

    @PostMapping("/check-in")
    public AppointmentDto checkIn(@Valid @RequestBody CheckInRequest req) {
        return appointmentService.checkIn(req.code());
    }

    // ---- logs ----

    @GetMapping("/audit-logs")
    public List<AuditLog> auditLogs(@RequestParam(required = false) String action,
                                    @RequestParam(defaultValue = "100") int limit) {
        return audit.recent(action, limit);
    }

    @GetMapping("/notifications/outbox")
    public List<OutboxDto> outbox() {
        return outbox.findAllByOrderByIdDesc(PageRequest.of(0, 100)).stream()
                .map(m -> new OutboxDto(m.getId(), m.getRecipient(), m.getSubject(), m.getStatus(), m.getAttempts(),
                        m.getNextAttemptAt(), m.getCreatedAt(), m.getSentAt(), m.getLastError()))
                .toList();
    }

    @GetMapping("/notifications/logs")
    public List<NotificationLog> notificationLogs() {
        return notificationLogs.findAllByOrderByAtDesc(PageRequest.of(0, 100));
    }
}
