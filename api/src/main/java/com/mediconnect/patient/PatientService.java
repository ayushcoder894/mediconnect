package com.mediconnect.patient;

import com.mediconnect.common.ApiException;
import com.mediconnect.security.CurrentUser;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patients;

    public record PatientDto(Long id, Long userId, String fullName, String email, String phone,
                             LocalDate dateOfBirth, Integer age, String gender, String bloodGroup,
                             BigDecimal heightCm, BigDecimal weightKg, BigDecimal bmi, String address,
                             String medicalHistory, String allergies, String smokingHistory,
                             boolean hypertension, boolean heartDisease) {

        public static PatientDto from(Patient p) {
            var u = p.getUser();
            return new PatientDto(p.getId(), u.getId(), u.getFullName(), u.getEmail(), u.getPhone(),
                    p.getDateOfBirth(), p.age(), p.getGender(), p.getBloodGroup(), p.getHeightCm(),
                    p.getWeightKg(), p.bmi(), p.getAddress(), p.getMedicalHistory(), p.getAllergies(),
                    p.getSmokingHistory(), p.isHypertension(), p.isHeartDisease());
        }
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 120) String fullName,
            @Pattern(regexp = "^[+0-9 -]{7,20}$", message = "Enter a valid phone number") String phone,
            @Past LocalDate dateOfBirth,
            @Pattern(regexp = "Male|Female|Other") String gender,
            @Pattern(regexp = "A\\+|A-|B\\+|B-|O\\+|O-|AB\\+|AB-") String bloodGroup,
            @DecimalMin("50") @DecimalMax("250") BigDecimal heightCm,
            @DecimalMin("2") @DecimalMax("300") BigDecimal weightKg,
            @Size(max = 255) String address,
            @Size(max = 2000) String medicalHistory,
            @Size(max = 500) String allergies,
            @Pattern(regexp = "never|former|current|not current|ever|No Info") String smokingHistory,
            boolean hypertension,
            boolean heartDisease) {
    }

    /** The patient record of the logged-in user. */
    public Patient currentPatient() {
        return patients.findByUserId(CurrentUser.get().id())
                .orElseThrow(() -> ApiException.forbidden("Only patients can do this"));
    }

    @Transactional(readOnly = true)
    public PatientDto me() {
        return PatientDto.from(currentPatient());
    }

    @Transactional
    public PatientDto update(UpdateProfileRequest req) {
        Patient p = currentPatient();
        p.getUser().setFullName(req.fullName().trim());
        p.getUser().setPhone(req.phone());
        p.setDateOfBirth(req.dateOfBirth());
        p.setGender(req.gender());
        p.setBloodGroup(req.bloodGroup());
        p.setHeightCm(req.heightCm());
        p.setWeightKg(req.weightKg());
        p.setAddress(req.address());
        p.setMedicalHistory(req.medicalHistory());
        p.setAllergies(req.allergies());
        p.setSmokingHistory(req.smokingHistory());
        p.setHypertension(req.hypertension());
        p.setHeartDisease(req.heartDisease());
        return PatientDto.from(p);
    }
}
