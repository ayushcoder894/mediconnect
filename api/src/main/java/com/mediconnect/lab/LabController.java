package com.mediconnect.lab;

import com.mediconnect.lab.LabDtos.*;
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
public class LabController {

    private final LabService labService;
    private final PdfService pdfService;

    @GetMapping("/api/lab-tests")
    public List<LabTestDto> catalog(@RequestParam(required = false) String category,
                                    @RequestParam(required = false) String q) {
        return labService.catalog(category, q, false);
    }

    @GetMapping("/api/lab-tests/categories")
    public List<String> categories() {
        return labService.categories();
    }

    @PostMapping("/api/lab-bookings")
    @PreAuthorize("hasRole('PATIENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public LabBookingDto book(@Valid @RequestBody BookLabRequest req) {
        return labService.book(req);
    }

    @GetMapping("/api/lab-bookings/mine")
    @PreAuthorize("hasRole('PATIENT')")
    public List<LabBookingDto> mine() {
        return labService.mine();
    }

    @PostMapping("/api/lab-bookings/{id}/cancel")
    @PreAuthorize("hasRole('PATIENT')")
    public LabBookingDto cancel(@PathVariable Long id) {
        return labService.cancel(id);
    }

    @PostMapping("/api/doctor/lab-orders")
    @PreAuthorize("hasRole('DOCTOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public LabBookingDto order(@Valid @RequestBody DoctorOrderRequest req) {
        return labService.orderByDoctor(req);
    }

    @GetMapping("/api/lab-bookings/{id}/report.pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> report(@PathVariable Long id) {
        byte[] pdf = pdfService.labReport(labService.loadForReport(id));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=lab-report-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
