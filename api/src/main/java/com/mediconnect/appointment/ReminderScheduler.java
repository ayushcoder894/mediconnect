package com.mediconnect.appointment;

import com.mediconnect.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;

/**
 * Every 15 minutes, queues a reminder for appointments starting in the next 23-25
 * hours. Windows overlap between runs on purpose; the idempotency key
 * "reminder:{id}" makes sure each patient still gets exactly one reminder.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final AppointmentRepository appointments;
    private final NotificationService notifications;

    @Scheduled(cron = "${app.reminders.cron:0 */15 * * * *}")
    @Transactional
    public void queueReminders() {
        LocalDateTime now = LocalDateTime.now();
        var due = appointments.findByStatusInAndSlotBetween(
                EnumSet.of(AppointmentStatus.BOOKED, AppointmentStatus.CONFIRMED),
                now.plusHours(23), now.plusHours(25));
        for (Appointment a : due) {
            notifications.enqueueEmail("reminder:" + a.getId(), a.getPatient().getUser().getEmail(),
                    "Reminder: appointment tomorrow with " + a.getDoctor().getUser().getFullName(),
                    "Hi " + a.getPatient().getUser().getFullName() + ",\n\nThis is a reminder of your appointment on "
                            + a.getSlotStart().format(AppointmentService.WHEN)
                            + ". Please arrive 10 minutes early and bring your QR slip.\n\nMediConnect");
        }
        if (!due.isEmpty()) {
            log.info("Queued reminders for {} appointment(s)", due.size());
        }
    }
}
