package com.mediconnect.appointment;

import com.mediconnect.pdf.PdfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final PdfService pdfService;

    @PostMapping("/api/appointments")
    @PreAuthorize("hasRole('PATIENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentDto book(@Valid @RequestBody AppointmentService.BookRequest req) {
        return appointmentService.book(req);
    }

    @GetMapping("/api/appointments/mine")
    @PreAuthorize("hasRole('PATIENT')")
    public List<AppointmentDto> mine() {
        return appointmentService.mine();
    }

    @GetMapping("/api/appointments/{id}")
    public AppointmentDto get(@PathVariable Long id) {
        return appointmentService.getForCurrentUser(id);
    }

    @PostMapping("/api/appointments/{id}/cancel")
    @PreAuthorize("hasRole('PATIENT')")
    public AppointmentDto cancel(@PathVariable Long id) {
        return appointmentService.cancel(id);
    }

    @PostMapping("/api/appointments/{id}/reschedule")
    @PreAuthorize("hasRole('PATIENT')")
    public AppointmentDto reschedule(@PathVariable Long id,
                                     @Valid @RequestBody AppointmentService.RescheduleRequest req) {
        return appointmentService.reschedule(id, req);
    }

    @GetMapping("/api/appointments/{id}/slip.pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> slip(@PathVariable Long id) {
        byte[] pdf = pdfService.appointmentSlip(appointmentService.loadForCurrentUser(id));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=appointment-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/api/doctor/appointments")
    @PreAuthorize("hasRole('DOCTOR')")
    public List<AppointmentDto> forDoctor(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return appointmentService.forDoctor(from, to);
    }

    @PatchMapping("/api/doctor/appointments/{id}/status")
    @PreAuthorize("hasRole('DOCTOR')")
    public AppointmentDto updateStatus(@PathVariable Long id,
                                       @Valid @RequestBody AppointmentService.StatusRequest req) {
        return appointmentService.updateStatusByDoctor(id, req.status());
    }
}
