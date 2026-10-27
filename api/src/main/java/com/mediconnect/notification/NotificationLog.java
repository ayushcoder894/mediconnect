package com.mediconnect.notification;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** One delivery attempt of an outbox message. Stored in MongoDB. */
@Document("notification_logs")
@Getter
@Setter
@NoArgsConstructor
public class NotificationLog {

    @Id
    private String id;

    @Indexed
    private Long outboxId;
    private String recipient;
    private String subject;
    private String outcome;
    private int attempt;
    private String error;

    @Indexed
    private Instant at = Instant.now();
}
