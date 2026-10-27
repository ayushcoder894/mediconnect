package com.mediconnect.review;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReviewRepository extends MongoRepository<Review, String> {

    List<Review> findByDoctorIdOrderByCreatedAtDesc(Long doctorId, Pageable pageable);

    boolean existsByAppointmentId(Long appointmentId);
}
