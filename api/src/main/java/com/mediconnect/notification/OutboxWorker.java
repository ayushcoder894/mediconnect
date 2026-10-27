package com.mediconnect.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Consumer side of the outbox: claims due messages, sends them, and retries
 * failures with exponential backoff (1, 2, 4, 8 minutes) before giving up.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxWorker {

    static final int MAX_ATTEMPTS = 5;
    private static final int BATCH_SIZE = 20;

    private final OutboxRepository outbox;
    private final NotificationLogRepository logs;
    private final EmailSender emailSender;
    private final TransactionTemplate tx;

    @Scheduled(fixedDelayString = "${app.outbox.poll-ms:10000}", initialDelay = 5000)
    public void processDue() {
        tx.executeWithoutResult(s -> outbox.releaseStuck(LocalDateTime.now().minusMinutes(10)));
        for (Long id : outbox.findDueIds(LocalDateTime.now(), PageRequest.of(0, BATCH_SIZE))) {
            Boolean claimed = tx.execute(s -> outbox.claim(id, LocalDateTime.now()) == 1);
            if (Boolean.TRUE.equals(claimed)) {
                deliver(id);
            }
        }
    }

    private void deliver(Long id) {
        OutboxMessage m = outbox.findById(id).orElseThrow();
        int attempt = m.getAttempts() + 1;
        String outcome;
        String error = null;
        try {
            outcome = emailSender.send(m.getRecipient(), m.getSubject(), m.getBody());
            m.setStatus(OutboxMessage.Status.SENT);
            m.setSentAt(LocalDateTime.now());
            m.setLastError(null);
        } catch (RuntimeException e) {
            error = e.getMessage();
            outcome = "ERROR";
            if (attempt >= MAX_ATTEMPTS) {
                m.setStatus(OutboxMessage.Status.FAILED);
            } else {
                m.setStatus(OutboxMessage.Status.PENDING);
                m.setNextAttemptAt(LocalDateTime.now().plus(backoff(attempt)));
            }
            m.setLastError(error == null ? null : error.substring(0, Math.min(error.length(), 500)));
            log.warn("Email {} attempt {} failed: {}", id, attempt, error);
        }
        m.setAttempts(attempt);
        tx.executeWithoutResult(s -> outbox.save(m));
        writeLog(m, attempt, outcome, error);
    }

    static Duration backoff(int attempt) {
        return Duration.ofMinutes(1L << (attempt - 1));
    }

    private void writeLog(OutboxMessage m, int attempt, String outcome, String error) {
        try {
            NotificationLog entry = new NotificationLog();
            entry.setOutboxId(m.getId());
            entry.setRecipient(m.getRecipient());
            entry.setSubject(m.getSubject());
            entry.setOutcome(outcome);
            entry.setAttempt(attempt);
            entry.setError(error);
            logs.save(entry);
        } catch (RuntimeException e) {
            log.warn("Could not write notification log: {}", e.getMessage());
        }
    }
}
