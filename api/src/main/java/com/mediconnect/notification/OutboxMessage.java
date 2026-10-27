package com.mediconnect.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_outbox")
@Getter
@Setter
@NoArgsConstructor
public class OutboxMessage {

    public enum Status { PENDING, SENDING, SENT, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    private String channel = "EMAIL";
    private String recipient;
    private String subject;
    private String body;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    private int attempts;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt = LocalDateTime.now();

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;
}
