package com.mediconnect.audit;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** An append-only record of who did what. Stored in MongoDB. */
@Document("audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    private String id;

    @Indexed
    private Long actorId;
    private String actorEmail;
    private String actorRole;

    @Indexed
    private String action;
    private String entityType;
    private Long entityId;
    private String details;

    @Indexed
    private Instant at = Instant.now();
}
