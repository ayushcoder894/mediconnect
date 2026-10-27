package com.mediconnect.appointment;

import org.junit.jupiter.api.Test;

import static com.mediconnect.appointment.AppointmentStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

class AppointmentStatusTest {

    @Test
    void bookedAppointmentCanBeConfirmedCheckedInOrCancelled() {
        assertThat(BOOKED.canMoveTo(CONFIRMED)).isTrue();
        assertThat(BOOKED.canMoveTo(CHECKED_IN)).isTrue();
        assertThat(BOOKED.canMoveTo(CANCELLED)).isTrue();
    }

    @Test
    void finishedAppointmentsAreFinal() {
        for (AppointmentStatus done : new AppointmentStatus[]{COMPLETED, CANCELLED, NO_SHOW}) {
            for (AppointmentStatus next : values()) {
                assertThat(done.canMoveTo(next)).as(done + " -> " + next).isFalse();
            }
        }
    }

    @Test
    void checkedInPatientCanOnlyBeCompleted() {
        assertThat(CHECKED_IN.canMoveTo(COMPLETED)).isTrue();
        assertThat(CHECKED_IN.canMoveTo(CANCELLED)).isFalse();
        assertThat(CHECKED_IN.canMoveTo(NO_SHOW)).isFalse();
    }
}
