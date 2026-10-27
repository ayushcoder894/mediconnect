package com.mediconnect.notification;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxBackoffTest {

    @Test
    void retryDelayDoublesEachAttempt() {
        assertThat(OutboxWorker.backoff(1)).isEqualTo(Duration.ofMinutes(1));
        assertThat(OutboxWorker.backoff(2)).isEqualTo(Duration.ofMinutes(2));
        assertThat(OutboxWorker.backoff(3)).isEqualTo(Duration.ofMinutes(4));
        assertThat(OutboxWorker.backoff(4)).isEqualTo(Duration.ofMinutes(8));
    }
}
