package com.mediconnect;

import com.mediconnect.doctor.DoctorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class ApiFlowIT extends IntegrationTestBase {

    @Autowired
    TestRestTemplate http;

    @Autowired
    DoctorRepository doctors;

    @Test
    void anonymousUsersCanBrowseButNotBook() {
        assertThat(http.getForEntity("/api/lab-tests", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(http.getForEntity("/api/public/stats", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        var booking = http.postForEntity("/api/appointments", json(Map.of("doctorId", 1), null), String.class);
        assertThat(booking.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void patientsCannotReachAdminEndpoints() {
        String token = registerPatient();
        var res = http.exchange("/api/admin/dashboard", HttpMethod.GET, json(null, token), String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void wrongPasswordGetsTheSameMessageAsUnknownEmail() {
        String email = "known-" + UUID.randomUUID() + "@example.com";
        registerPatient(email);
        var wrongPassword = http.postForEntity("/api/auth/login",
                json(Map.of("email", email, "password", "Wrong1234"), null), Map.class);
        var unknownEmail = http.postForEntity("/api/auth/login",
                json(Map.of("email", "nobody@example.com", "password", "Wrong1234"), null), Map.class);
        assertThat(wrongPassword.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(wrongPassword.getBody().get("message")).isEqualTo(unknownEmail.getBody().get("message"));
    }

    @Test
    void unapprovedDoctorsAreHiddenFromSearch() {
        Long doctorId = registerDoctor(false);
        var res = http.getForEntity("/api/doctors/" + doctorId, String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * Eight patients press "Book" for the same slot at the same moment. Exactly one
     * gets the appointment; the other seven get a 409, never a double-booking.
     */
    @Test
    void concurrentBookingsForOneSlotProduceExactlyOneAppointment() throws Exception {
        Long doctorId = registerDoctor(true);
        String slot = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY)).atTime(10, 30).toString();
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            tokens.add(registerPatient());
        }

        ExecutorService pool = Executors.newFixedThreadPool(tokens.size());
        CountDownLatch start = new CountDownLatch(1);
        List<CompletableFuture<HttpStatusCode>> results = tokens.stream()
                .map(token -> CompletableFuture.supplyAsync(() -> {
                    await(start);
                    return http.postForEntity("/api/appointments",
                            json(Map.of("doctorId", doctorId, "slotStart", slot), token), String.class).getStatusCode();
                }, pool))
                .toList();
        start.countDown();
        List<HttpStatusCode> codes = results.stream().map(CompletableFuture::join).toList();
        pool.shutdown();

        assertThat(codes).filteredOn(c -> c.value() == 201).hasSize(1);
        assertThat(codes).filteredOn(c -> c.value() == 409).hasSize(7);

        var slots = http.getForEntity("/api/doctors/" + doctorId + "/slots?date=" + slot.substring(0, 10), List.class);
        List<Map<String, Object>> list = slots.getBody();
        assertThat(list).filteredOn(s -> String.valueOf(s.get("start")).startsWith(slot))
                .singleElement().extracting(s -> s.get("available")).isEqualTo(false);
    }

    @Test
    void cancelledSlotCanBeBookedAgain() {
        Long doctorId = registerDoctor(true);
        String slot = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.THURSDAY)).atTime(11, 0).toString();
        String first = registerPatient();
        String second = registerPatient();

        var booked = http.postForEntity("/api/appointments", json(Map.of("doctorId", doctorId, "slotStart", slot), first), Map.class);
        assertThat(booked.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Object id = booked.getBody().get("id");

        var cancel = http.postForEntity("/api/appointments/" + id + "/cancel", json(null, first), String.class);
        assertThat(cancel.getStatusCode()).isEqualTo(HttpStatus.OK);

        var rebooked = http.postForEntity("/api/appointments", json(Map.of("doctorId", doctorId, "slotStart", slot), second), String.class);
        assertThat(rebooked.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    // ---- helpers ----

    private String registerPatient() {
        return registerPatient("patient-" + UUID.randomUUID() + "@example.com");
    }

    private String registerPatient(String email) {
        var res = http.postForEntity("/api/auth/register/patient", json(Map.of(
                "fullName", "Test Patient", "email", email, "password", "Passw0rd1"), null), String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return tokenFrom(res);
    }

    private Long registerDoctor(boolean approve) {
        var res = http.postForEntity("/api/auth/register/doctor", json(Map.of(
                "fullName", "Dr. Test", "email", "doc-" + UUID.randomUUID() + "@example.com", "password", "Passw0rd1",
                "specialization", "General Medicine", "qualification", "MBBS", "experienceYears", 5,
                "consultationFee", 500), null), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long id = ((Number) res.getBody().get("profileId")).longValue();
        if (approve) {
            var d = doctors.findById(id).orElseThrow();
            d.setApproved(true);
            doctors.save(d);
        }
        return id;
    }

    private static String tokenFrom(ResponseEntity<?> res) {
        String cookie = res.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        return cookie.substring(cookie.indexOf('=') + 1, cookie.indexOf(';'));
    }

    private static HttpEntity<Object> json(Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return new HttpEntity<>(body, headers);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
