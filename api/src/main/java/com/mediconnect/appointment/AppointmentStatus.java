package com.mediconnect.appointment;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum AppointmentStatus {
    BOOKED, CONFIRMED, CHECKED_IN, COMPLETED, CANCELLED, NO_SHOW;

    private static final Map<AppointmentStatus, Set<AppointmentStatus>> ALLOWED = Map.of(
            BOOKED, EnumSet.of(CONFIRMED, CHECKED_IN, CANCELLED, NO_SHOW),
            CONFIRMED, EnumSet.of(CHECKED_IN, COMPLETED, CANCELLED, NO_SHOW),
            CHECKED_IN, EnumSet.of(COMPLETED),
            COMPLETED, EnumSet.noneOf(AppointmentStatus.class),
            CANCELLED, EnumSet.noneOf(AppointmentStatus.class),
            NO_SHOW, EnumSet.noneOf(AppointmentStatus.class));

    public boolean canMoveTo(AppointmentStatus next) {
        return ALLOWED.get(this).contains(next);
    }

    public boolean isActive() {
        return this == BOOKED || this == CONFIRMED || this == CHECKED_IN;
    }
}
