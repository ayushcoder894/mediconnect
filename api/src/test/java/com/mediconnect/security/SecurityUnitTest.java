package com.mediconnect.security;

import com.mediconnect.common.AppProperties;
import com.mediconnect.user.Role;
import com.mediconnect.user.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityUnitTest {

    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtYXQtbGVhc3QtMzItYnl0ZXM=";

    private final JwtService jwt = new JwtService(new AppProperties(
            new AppProperties.Jwt(SECRET, 60, false), new AppProperties.Cors(List.of()),
            new AppProperties.AiService("http://localhost", 5), new AppProperties.Mail(false, ""), false));

    private User user() {
        User u = new User("asha@example.com", "hash", "Asha Rao", null, Role.PATIENT);
        u.setId(7L);
        return u;
    }

    @Test
    void tokenRoundTripsTheUser() {
        AuthUser parsed = jwt.parse(jwt.issue(user())).orElseThrow();
        assertThat(parsed).isEqualTo(new AuthUser(7L, "asha@example.com", "Asha Rao", Role.PATIENT));
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwt.issue(user());
        String tampered = token.substring(0, token.length() - 3) + (token.endsWith("A") ? "BBB" : "AAA");
        assertThat(jwt.parse(tampered)).isEmpty();
        assertThat(jwt.parse("not-a-jwt")).isEmpty();
    }

    @Test
    void accountLocksAfterFiveFailuresAndUnlocksOnSuccess() {
        LoginAttemptService attempts = new LoginAttemptService();
        for (int i = 0; i < 4; i++) {
            attempts.recordFailure("A@example.com ");
        }
        assertThat(attempts.isLocked("a@example.com")).isFalse();
        attempts.recordFailure("a@example.com");
        assertThat(attempts.isLocked("a@example.com")).isTrue();

        LoginAttemptService fresh = new LoginAttemptService();
        fresh.recordFailure("b@example.com");
        fresh.recordSuccess("b@example.com");
        for (int i = 0; i < 4; i++) {
            fresh.recordFailure("b@example.com");
        }
        assertThat(fresh.isLocked("b@example.com")).isFalse();
    }
}
