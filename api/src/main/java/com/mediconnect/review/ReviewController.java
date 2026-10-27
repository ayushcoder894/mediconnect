package com.mediconnect.review;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/api/appointments/{appointmentId}/review")
    @PreAuthorize("hasRole('PATIENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewService.ReviewDto create(@PathVariable Long appointmentId,
                                          @Valid @RequestBody ReviewService.CreateReviewRequest req) {
        return reviewService.create(appointmentId, req);
    }

    @GetMapping("/api/doctors/{doctorId}/reviews")
    public List<ReviewService.ReviewDto> forDoctor(@PathVariable Long doctorId) {
        return reviewService.forDoctor(doctorId);
    }
}
