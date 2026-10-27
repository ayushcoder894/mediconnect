package com.mediconnect;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Map;

/**
 * Starts real MySQL and MongoDB containers once and shares them across all
 * integration tests, so Flyway migrations, constraints and queries are tested
 * against the same engines as production.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "app.seed-demo-data=false",
        "app.outbox.poll-ms=600000",
        "app.reminders.cron=-"
})
public abstract class IntegrationTestBase {

    // Data directory in memory: a throwaway test database does not need durable disk
    // writes, and it cuts MySQL's first-start initialisation from minutes to seconds.
    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withTmpFs(Map.of("/var/lib/mysql", "rw"))
            .withStartupTimeout(Duration.ofMinutes(5));

    @Container
    @ServiceConnection
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7");
}
