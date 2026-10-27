package com.mediconnect.prescription;

import com.mediconnect.pdf.PdfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final PdfService pdfService;

    @PostMapping("/api/doctor/appointments/{appointmentId}/prescription")
    @PreAuthorize("hasRole('DOCTOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public PrescriptionService.PrescriptionDto create(@PathVariable Long appointmentId,
                                                      @Valid @RequestBody PrescriptionService.CreateRequest req) {
        return prescriptionService.create(appointmentId, req);
    }

    @GetMapping("/api/prescriptions/mine")
    @PreAuthorize("hasRole('PATIENT')")
    public List<PrescriptionService.PrescriptionDto> mine() {
        return prescriptionService.mine();
    }

    @GetMapping("/api/appointments/{appointmentId}/prescription")
    public PrescriptionService.PrescriptionDto byAppointment(@PathVariable Long appointmentId) {
        return prescriptionService.byAppointment(appointmentId);
    }

    @GetMapping("/api/prescriptions/{id}/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        byte[] pdf = pdfService.prescription(prescriptionService.loadForCurrentUser(id));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=prescription-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
