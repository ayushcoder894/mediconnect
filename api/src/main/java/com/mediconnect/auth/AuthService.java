package com.mediconnect.auth;

import com.mediconnect.audit.AuditService;
import com.mediconnect.common.ApiException;
import com.mediconnect.doctor.Doctor;
import com.mediconnect.doctor.DoctorAvailability;
import com.mediconnect.doctor.DoctorRepository;
import com.mediconnect.hospital.HospitalService;
import com.mediconnect.patient.Patient;
import com.mediconnect.patient.PatientRepository;
import com.mediconnect.security.AuthUser;
import com.mediconnect.security.LoginAttemptService;
import com.mediconnect.user.Role;
import com.mediconnect.user.User;
import com.mediconnect.user.UserRepository;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String PASSWORD_RULE = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$";
    private static final String PASSWORD_MESSAGE = "Use at least 8 characters with a letter and a number";

    private final UserRepository users;
    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final HospitalService hospitalService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttempts;
    private final AuditService audit;

    public record RegisterPatientRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MESSAGE) String password,
            @Pattern(regexp = "^[+0-9 -]{7,20}$", message = "Enter a valid phone number") String phone,
            @Past LocalDate dateOfBirth,
            @Pattern(regexp = "Male|Female|Other") String gender) {
    }

    public record RegisterDoctorRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MESSAGE) String password,
            @Pattern(regexp = "^[+0-9 -]{7,20}$", message = "Enter a valid phone number") String phone,
            @NotBlank @Size(max = 80) String specialization,
            @NotBlank @Size(max = 160) String qualification,
            @Min(0) @Max(60) int experienceYears,
            @NotNull @DecimalMin("0") @DecimalMax("100000") BigDecimal consultationFee,
            Long hospitalId) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    /** What the client needs to know about the logged-in user. */
    public record MeDto(Long id, String email, String fullName, Role role, Long profileId, Boolean approved) {
    }

    @Transactional
    public User registerPatient(RegisterPatientRequest req) {
        User user = createUser(req.email(), req.password(), req.fullName(), req.phone(), Role.PATIENT);
        Patient patient = new Patient(user);
        patient.setDateOfBirth(req.dateOfBirth());
        patient.setGender(req.gender());
        patient.setSmokingHistory("No Info");
        patients.save(patient);
        audit.record("REGISTER_PATIENT", "User", user.getId(), user.getEmail());
        return user;
    }

    /** Doctors start unapproved and only appear in search once an admin approves them. */
    @Transactional
    public User registerDoctor(RegisterDoctorRequest req) {
        User user = createUser(req.email(), req.password(), req.fullName(), req.phone(), Role.DOCTOR);
        Doctor d = new Doctor();
        d.setUser(user);
        d.setSpecialization(req.specialization().trim());
        d.setQualification(req.qualification().trim());
        d.setExperienceYears(req.experienceYears());
        d.setConsultationFee(req.consultationFee());
        d.setHospital(req.hospitalId() == null ? null : hospitalService.getEntity(req.hospitalId()));
        for (DayOfWeek day : EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)) {
            d.getAvailability().add(new DoctorAvailability(d, day, LocalTime.of(10, 0), LocalTime.of(14, 0)));
        }
        doctors.save(d);
        audit.record("REGISTER_DOCTOR", "User", user.getId(), user.getEmail());
        return user;
    }

    private User createUser(String email, String password, String fullName, String phone, Role role) {
        String normalized = email.trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(normalized)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        return users.save(new User(normalized, passwordEncoder.encode(password), fullName.trim(), phone, role));
    }

    @Transactional(readOnly = true)
    public User login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        if (loginAttempts.isLocked(email)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed attempts. Please try again in 15 minutes.");
        }
        User user = users.findByEmailIgnoreCase(email).orElse(null);
        // Same message for unknown email and wrong password, so accounts cannot be enumerated
        if (user == null || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            loginAttempts.recordFailure(email);
            throw new BadCredentialsException("Incorrect email or password");
        }
        if (!user.isEnabled()) {
            throw ApiException.forbidden("This account has been disabled. Please contact support.");
        }
        loginAttempts.recordSuccess(email);
        return user;
    }

    @Transactional(readOnly = true)
    public MeDto me(AuthUser auth) {
        User user = users.findById(auth.id()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Please log in"));
        return toMe(user);
    }

    @Transactional(readOnly = true)
    public MeDto toMe(User user) {
        return switch (user.getRole()) {
            case PATIENT -> new MeDto(user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
                    patients.findByUserId(user.getId()).map(Patient::getId).orElse(null), null);
            case DOCTOR -> {
                Doctor d = doctors.findByUserId(user.getId()).orElse(null);
                yield new MeDto(user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
                        d == null ? null : d.getId(), d != null && d.isApproved());
            }
            case ADMIN -> new MeDto(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), null, null);
        };
    }
}
