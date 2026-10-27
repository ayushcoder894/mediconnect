package com.mediconnect.review;

import com.mediconnect.appointment.Appointment;
import com.mediconnect.appointment.AppointmentRepository;
import com.mediconnect.appointment.AppointmentStatus;
import com.mediconnect.audit.AuditService;
import com.mediconnect.common.ApiException;
import com.mediconnect.patient.PatientService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviews;
    private final AppointmentRepository appointments;
    private final PatientService patientService;
    private final AuditService audit;

    public record CreateReviewRequest(@Min(1) @Max(5) int rating, @Size(max = 1000) String comment,
                                      @Size(max = 5) List<@Size(max = 30) String> tags) {
    }

    public record ReviewDto(String id, String patientName, int rating, String comment, List<String> tags,
                            Instant createdAt) {

        static ReviewDto from(Review r) {
            return new ReviewDto(r.getId(), r.getPatientName(), r.getRating(), r.getComment(), r.getTags(),
                    r.getCreatedAt());
        }
    }

    /**
     * Saves the review in MongoDB and updates the doctor's rating in MySQL. The Mongo
     * write happens inside the MySQL transaction, so if it fails the rating update
     * rolls back and the two stores stay consistent.
     */
    @Transactional
    public ReviewDto create(Long appointmentId, CreateReviewRequest req) {
        var patient = patientService.currentPatient();
        Appointment a = appointments.findDetailed(appointmentId)
                .filter(x -> x.getPatient().getId().equals(patient.getId()))
                .orElseThrow(() -> ApiException.notFound("Appointment"));
        if (a.getStatus() != AppointmentStatus.COMPLETED) {
            throw ApiException.badRequest("You can review a visit once it is completed");
        }
        if (a.isReviewed() || reviews.existsByAppointmentId(appointmentId)) {
            throw ApiException.conflict("You have already reviewed this visit");
        }
        a.setReviewed(true);
        a.getDoctor().addRating(req.rating());

        Review r = new Review();
        r.setDoctorId(a.getDoctor().getId());
        r.setPatientId(patient.getId());
        r.setPatientName(firstNameAndInitial(patient.getUser().getFullName()));
        r.setAppointmentId(appointmentId);
        r.setRating(req.rating());
        r.setComment(req.comment() == null ? null : req.comment().trim());
        r.setTags(req.tags());
        reviews.save(r);
        audit.record("REVIEW_DOCTOR", "Doctor", a.getDoctor().getId(), req.rating() + " stars");
        return ReviewDto.from(r);
    }

    public List<ReviewDto> forDoctor(Long doctorId) {
        return reviews.findByDoctorIdOrderByCreatedAtDesc(doctorId, PageRequest.of(0, 20)).stream()
                .map(ReviewDto::from).toList();
    }

    /** "Ananya Sharma" becomes "Ananya S." so public reviews do not expose full names. */
    static String firstNameAndInitial(String fullName) {
        String[] parts = fullName.trim().split("\\s+");
        return parts.length == 1 ? parts[0] : parts[0] + " " + parts[parts.length - 1].charAt(0) + ".";
    }
}
