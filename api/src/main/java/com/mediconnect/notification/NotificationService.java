package com.mediconnect.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Producer side of the transactional outbox. Messages are inserted in the caller's
 * transaction, so an email exists if and only if the business change committed.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final OutboxRepository outbox;

    /**
     * Queues an email. The idempotency key makes this safe to call more than once
     * for the same event (e.g. the reminder job running twice): duplicates are ignored.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueEmail(String idempotencyKey, String recipient, String subject, String body) {
        if (outbox.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }
        OutboxMessage m = new OutboxMessage();
        m.setIdempotencyKey(idempotencyKey);
        m.setRecipient(recipient);
        m.setSubject(subject);
        m.setBody(body);
        outbox.save(m);
    }
}
