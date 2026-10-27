package com.mediconnect.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Locks an account for 15 minutes after 5 consecutive failed logins to slow down
 * password guessing. In-memory, so it is per instance; a multi-instance deployment
 * would move this to Redis.
 */
@Service
public class LoginAttemptService {

    static final int MAX_ATTEMPTS = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private record Attempts(int count, Instant lockedUntil) {}

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    public boolean isLocked(String email) {
        Attempts a = attempts.get(key(email));
        return a != null && a.lockedUntil() != null && a.lockedUntil().isAfter(Instant.now());
    }

    public void recordFailure(String email) {
        attempts.compute(key(email), (k, a) -> {
            int count = (a == null || a.lockedUntil() != null) ? 1 : a.count() + 1;
            Instant lockedUntil = count >= MAX_ATTEMPTS ? Instant.now().plus(LOCK_DURATION) : null;
            return new Attempts(count, lockedUntil);
        });
    }

    public void recordSuccess(String email) {
        attempts.remove(key(email));
    }

    private String key(String email) {
        return email.trim().toLowerCase();
    }
}
