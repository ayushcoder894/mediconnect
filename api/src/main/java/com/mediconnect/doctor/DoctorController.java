package com.mediconnect.doctor;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@Validated
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping
    public List<DoctorService.DoctorSummary> search(@RequestParam(required = false) String specialization,
                                                    @RequestParam(required = false) String q,
                                                    @RequestParam(required = false) String city,
                                                    @RequestParam(required = false) BigDecimal maxFee) {
        return doctorService.search(specialization, q, city, maxFee);
    }

    @GetMapping("/specializations")
    public List<String> specializations() {
        return doctorService.specializations();
    }

    @GetMapping("/{id}")
    public DoctorService.DoctorDetail get(@PathVariable Long id) {
        return doctorService.get(id);
    }

    @GetMapping("/{id}/slots")
    public List<DoctorService.SlotDto> slots(@PathVariable Long id,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return doctorService.slots(id, date);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('DOCTOR')")
    public DoctorService.DoctorDetail me() {
        return doctorService.me();
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('DOCTOR')")
    public DoctorService.DoctorDetail updateMe(@Valid @RequestBody DoctorService.UpdateDoctorRequest req) {
        return doctorService.updateMe(req);
    }

    @PutMapping("/me/availability")
    @PreAuthorize("hasRole('DOCTOR')")
    public DoctorService.DoctorDetail availability(@Valid @RequestBody List<DoctorService.AvailabilityDto> hours) {
        return doctorService.replaceAvailability(hours);
    }
}
