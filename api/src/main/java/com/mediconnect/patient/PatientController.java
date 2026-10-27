package com.mediconnect.patient;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patients/me")
@PreAuthorize("hasRole('PATIENT')")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @GetMapping
    public PatientService.PatientDto me() {
        return patientService.me();
    }

    @PutMapping
    public PatientService.PatientDto update(@Valid @RequestBody PatientService.UpdateProfileRequest req) {
        return patientService.update(req);
    }
}
