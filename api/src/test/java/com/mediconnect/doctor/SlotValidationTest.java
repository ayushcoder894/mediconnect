package com.mediconnect.doctor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

import static org.assertj.core.api.Assertions.assertThat;

class SlotValidationTest {

    private Doctor doctor;
    private LocalDate monday;

    @BeforeEach
    void setUp() {
        doctor = new Doctor();
        doctor.setSlotMinutes(30);
        doctor.getAvailability().add(new DoctorAvailability(doctor, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)));
        monday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    @Test
    void acceptsSlotsOnTheGridWithinWorkingHours() {
        assertThat(DoctorService.isValidSlot(doctor, monday.atTime(9, 0))).isTrue();
        assertThat(DoctorService.isValidSlot(doctor, monday.atTime(11, 30))).isTrue();
    }

    @Test
    void rejectsSlotsOffTheGrid() {
        assertThat(DoctorService.isValidSlot(doctor, monday.atTime(9, 15))).isFalse();
    }

    @Test
    void rejectsSlotsThatWouldRunPastClosingTime() {
        assertThat(DoctorService.isValidSlot(doctor, monday.atTime(12, 0))).isFalse();
    }

    @Test
    void rejectsDaysTheDoctorDoesNotWork() {
        assertThat(DoctorService.isValidSlot(doctor, monday.plusDays(1).atTime(9, 0))).isFalse();
    }

    @Test
    void ratingIsARunningAverage() {
        doctor.addRating(5);
        doctor.addRating(4);
        doctor.addRating(3);
        assertThat(doctor.getRatingAvg()).isEqualByComparingTo("4.00");
        assertThat(doctor.getRatingCount()).isEqualTo(3);
    }
}
