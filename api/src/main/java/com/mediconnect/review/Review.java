package com.mediconnect.review;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/** A patient's review of a completed visit. Stored in MongoDB. */
@Document("reviews")
@Getter
@Setter
@NoArgsConstructor
public class Review {

    @Id
    private String id;

    @Indexed
    private Long doctorId;

    private Long patientId;
    private String patientName;

    @Indexed(unique = true)
    private Long appointmentId;

    private int rating;
    private String comment;
    private List<String> tags;
    private Instant createdAt = Instant.now();
}
