package com.mediconnect.admin;

import com.mediconnect.notification.OutboxMessage;
import com.mediconnect.notification.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Read-only reporting queries for the admin dashboard. Plain SQL through
 * JdbcTemplate: aggregations are clearer in SQL than in JPQL and need no entities.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final int DAYS = 30;

    private final JdbcTemplate jdbc;
    private final OutboxRepository outbox;

    public record Totals(long patients, long doctors, long pendingDoctors, long appointments,
                         BigDecimal consultationRevenue, BigDecimal labRevenue, long labBookings,
                         double noShowRate, long todayAppointments, long todayCheckedIn,
                         long emailsPending, long emailsFailed) {
    }

    public record DayPoint(String date, long booked, long completed, long cancelled) {
    }

    public record NamedValue(String name, long count, BigDecimal amount) {
    }

    public record Dashboard(Totals totals, List<DayPoint> appointmentsPerDay, List<NamedValue> statusBreakdown,
                            List<NamedValue> revenueByDoctor, List<NamedValue> specializationDemand,
                            List<NamedValue> topLabTests, List<NamedValue> riskBands) {
    }

    public Dashboard dashboard() {
        return new Dashboard(totals(), perDay(), named("""
                        select status as name, count(*) as cnt, sum(fee) as amt from appointments group by status
                        order by cnt desc"""),
                named("""
                        select u.full_name as name, count(*) as cnt, sum(a.fee) as amt
                        from appointments a join doctors d on d.id = a.doctor_id join users u on u.id = d.user_id
                        where a.status = 'COMPLETED' group by d.id, u.full_name order by amt desc limit 8"""),
                named("""
                        select d.specialization as name, count(*) as cnt, sum(a.fee) as amt
                        from appointments a join doctors d on d.id = a.doctor_id
                        where a.status <> 'CANCELLED' group by d.specialization order by cnt desc"""),
                named("""
                        select t.name as name, count(*) as cnt, sum(b.price) as amt
                        from lab_bookings b join lab_tests t on t.id = b.lab_test_id
                        where b.status <> 'CANCELLED' group by t.id, t.name order by cnt desc limit 6"""),
                named("""
                        select risk_band as name, count(*) as cnt, null as amt from risk_assessments
                        group by risk_band order by field(risk_band, 'LOW', 'MODERATE', 'HIGH', 'VERY_HIGH')"""));
    }

    private Totals totals() {
        long completed = count("select count(*) from appointments where status = 'COMPLETED'");
        long noShow = count("select count(*) from appointments where status = 'NO_SHOW'");
        double noShowRate = completed + noShow == 0 ? 0 : (double) noShow / (completed + noShow);
        return new Totals(
                count("select count(*) from patients"),
                count("select count(*) from doctors where approved = true"),
                count("select count(*) from doctors where approved = false"),
                count("select count(*) from appointments where status <> 'CANCELLED'"),
                amount("select coalesce(sum(fee), 0) from appointments where status = 'COMPLETED'"),
                amount("select coalesce(sum(price), 0) from lab_bookings where status <> 'CANCELLED'"),
                count("select count(*) from lab_bookings where status <> 'CANCELLED'"),
                Math.round(noShowRate * 1000) / 1000.0,
                count("select count(*) from appointments where date(slot_start) = ? and status <> 'CANCELLED'",
                        LocalDate.now()),
                count("select count(*) from appointments where date(slot_start) = ? and checked_in_at is not null",
                        LocalDate.now()),
                outbox.countByStatus(OutboxMessage.Status.PENDING),
                outbox.countByStatus(OutboxMessage.Status.FAILED));
    }

    /** Last 30 days including today, with zero-filled gaps so the chart has no holes. */
    private List<DayPoint> perDay() {
        LocalDate start = LocalDate.now().minusDays(DAYS - 1L);
        Map<LocalDate, DayPoint> byDay = new HashMap<>();
        jdbc.query("""
                select date(slot_start) as d, count(*) as booked,
                       sum(status = 'COMPLETED') as completed, sum(status = 'CANCELLED') as cancelled
                from appointments where slot_start >= ? and slot_start < ?
                group by date(slot_start)""", rs -> {
            LocalDate d = rs.getDate("d").toLocalDate();
            byDay.put(d, new DayPoint(d.toString(), rs.getLong("booked"), rs.getLong("completed"), rs.getLong("cancelled")));
        }, start, LocalDate.now().plusDays(1));
        List<DayPoint> points = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(LocalDate.now()); d = d.plusDays(1)) {
            points.add(byDay.getOrDefault(d, new DayPoint(d.toString(), 0, 0, 0)));
        }
        return points;
    }

    private List<NamedValue> named(String sql) {
        return jdbc.query(sql, (rs, i) -> new NamedValue(rs.getString("name"), rs.getLong("cnt"),
                rs.getBigDecimal("amt")));
    }

    private long count(String sql, Object... args) {
        Long v = jdbc.queryForObject(sql, Long.class, args);
        return v == null ? 0 : v;
    }

    private BigDecimal amount(String sql) {
        BigDecimal v = jdbc.queryForObject(sql, BigDecimal.class);
        return v == null ? BigDecimal.ZERO : v;
    }
}
