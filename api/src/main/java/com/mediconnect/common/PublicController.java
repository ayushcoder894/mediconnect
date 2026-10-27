package com.mediconnect.common;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Headline numbers for the public landing page. */
@RestController
@RequiredArgsConstructor
public class PublicController {

    private final JdbcTemplate jdbc;

    public record PublicStats(long doctors, long specializations, long hospitals, long labTests,
                              long patients, long completedVisits) {
    }

    @GetMapping("/api/public/stats")
    public PublicStats stats() {
        return new PublicStats(
                count("select count(*) from doctors where approved = true"),
                count("select count(distinct specialization) from doctors where approved = true"),
                count("select count(*) from hospitals"),
                count("select count(*) from lab_tests where active = true"),
                count("select count(*) from patients"),
                count("select count(*) from appointments where status = 'COMPLETED'"));
    }

    private long count(String sql) {
        Long v = jdbc.queryForObject(sql, Long.class);
        return v == null ? 0 : v;
    }
}
