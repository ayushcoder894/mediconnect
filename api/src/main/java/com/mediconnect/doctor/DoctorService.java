package com.mediconnect.doctor;

import com.mediconnect.appointment.AppointmentRepository;
import com.mediconnect.common.ApiException;
import com.mediconnect.hospital.HospitalService;
import com.mediconnect.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;
    private final HospitalService hospitalService;

    public record DoctorSummary(Long id, String name, String specialization, String qualification,
                                int experienceYears, BigDecimal fee, BigDecimal rating, int ratingCount,
                                Long hospitalId, String hospitalName, String city, String bio,
                                int slotMinutes, boolean approved) {

        public static DoctorSummary from(Doctor d) {
            var h = d.getHospital();
            return new DoctorSummary(d.getId(), d.getUser().getFullName(), d.getSpecialization(),
                    d.getQualification(), d.getExperienceYears(), d.getConsultationFee(), d.getRatingAvg(),
                    d.getRatingCount(), h == null ? null : h.getId(), h == null ? null : h.getName(),
                    h == null ? null : h.getCity(), d.getBio(), d.getSlotMinutes(), d.isApproved());
        }
    }

    public record AvailabilityDto(@NotNull DayOfWeek dayOfWeek, @NotNull LocalTime startTime,
                                  @NotNull LocalTime endTime) {
    }

    public record DoctorDetail(DoctorSummary doctor, List<AvailabilityDto> availability) {
    }

    public record SlotDto(LocalDateTime start, LocalDateTime end, boolean available) {
    }

    public record UpdateDoctorRequest(
            @NotBlank @Size(max = 160) String qualification,
            @Min(0) @Max(60) int experienceYears,
            @NotNull @DecimalMin("0") @DecimalMax("100000") BigDecimal consultationFee,
            @Size(max = 2000) String bio,
            @Min(10) @Max(120) int slotMinutes,
            Long hospitalId) {
    }

    @Transactional(readOnly = true)
    public List<DoctorSummary> search(String specialization, String q, String city, BigDecimal maxFee) {
        String like = (q == null || q.isBlank()) ? null : "%" + q.trim().toLowerCase() + "%";
        return doctors.search(blank(specialization), like, blank(city), maxFee).stream()
                .map(DoctorSummary::from).toList();
    }

    @Transactional(readOnly = true)
    public List<String> specializations() {
        return doctors.findSpecializations();
    }

    @Transactional(readOnly = true)
    public DoctorDetail get(Long id) {
        Doctor d = doctors.findWithUser(id)
                .filter(Doctor::isApproved)
                .orElseThrow(() -> ApiException.notFound("Doctor"));
        return detail(d);
    }

    /**
     * Bookable slots for one day: the doctor's working hours for that weekday, cut
     * into slot-length pieces, minus slots already taken or already in the past.
     */
    @Transactional(readOnly = true)
    public List<SlotDto> slots(Long doctorId, LocalDate date) {
        Doctor d = doctors.findById(doctorId).filter(Doctor::isApproved)
                .orElseThrow(() -> ApiException.notFound("Doctor"));
        Optional<DoctorAvailability> hours = d.getAvailability().stream()
                .filter(a -> a.getDayOfWeek() == date.getDayOfWeek()).findFirst();
        if (hours.isEmpty()) {
            return List.of();
        }
        Set<LocalDateTime> taken = new HashSet<>(appointments.findTakenSlots(
                doctorId, date.atStartOfDay(), date.plusDays(1).atStartOfDay()));
        LocalDateTime now = LocalDateTime.now();
        List<SlotDto> result = new ArrayList<>();
        LocalDateTime t = date.atTime(hours.get().getStartTime());
        LocalDateTime end = date.atTime(hours.get().getEndTime());
        while (!t.plusMinutes(d.getSlotMinutes()).isAfter(end)) {
            boolean available = t.isAfter(now) && !taken.contains(t);
            result.add(new SlotDto(t, t.plusMinutes(d.getSlotMinutes()), available));
            t = t.plusMinutes(d.getSlotMinutes());
        }
        return result;
    }

    /** True if the time is the start of one of the doctor's slots on that day. */
    public static boolean isValidSlot(Doctor d, LocalDateTime start) {
        return d.getAvailability().stream()
                .filter(a -> a.getDayOfWeek() == start.getDayOfWeek())
                .anyMatch(a -> {
                    LocalTime time = start.toLocalTime();
                    long offset = Duration.between(a.getStartTime(), time).toMinutes();
                    return !time.isBefore(a.getStartTime())
                            && !time.plusMinutes(d.getSlotMinutes()).isAfter(a.getEndTime())
                            && offset % d.getSlotMinutes() == 0
                            && start.getSecond() == 0;
                });
    }

    public Doctor currentDoctor() {
        return doctors.findByUserId(CurrentUser.get().id())
                .orElseThrow(() -> ApiException.forbidden("Only doctors can do this"));
    }

    @Transactional(readOnly = true)
    public DoctorDetail me() {
        return detail(currentDoctor());
    }

    @Transactional
    public DoctorDetail updateMe(UpdateDoctorRequest req) {
        Doctor d = currentDoctor();
        d.setQualification(req.qualification());
        d.setExperienceYears(req.experienceYears());
        d.setConsultationFee(req.consultationFee());
        d.setBio(req.bio());
        d.setSlotMinutes(req.slotMinutes());
        d.setHospital(req.hospitalId() == null ? null : hospitalService.getEntity(req.hospitalId()));
        return detail(d);
    }

    @Transactional
    public DoctorDetail replaceAvailability(List<@Valid AvailabilityDto> hours) {
        Set<DayOfWeek> seen = EnumSet.noneOf(DayOfWeek.class);
        for (AvailabilityDto h : hours) {
            if (!h.endTime().isAfter(h.startTime())) {
                throw ApiException.badRequest("End time must be after start time on " + h.dayOfWeek());
            }
            if (!seen.add(h.dayOfWeek())) {
                throw ApiException.badRequest("Only one time range per day is supported");
            }
        }
        Doctor d = currentDoctor();
        d.getAvailability().clear();
        doctors.flush();
        hours.forEach(h -> d.getAvailability()
                .add(new DoctorAvailability(d, h.dayOfWeek(), h.startTime(), h.endTime())));
        return detail(d);
    }

    private DoctorDetail detail(Doctor d) {
        List<AvailabilityDto> hours = d.getAvailability().stream()
                .sorted(Comparator.comparing(DoctorAvailability::getDayOfWeek))
                .map(a -> new AvailabilityDto(a.getDayOfWeek(), a.getStartTime(), a.getEndTime()))
                .toList();
        return new DoctorDetail(DoctorSummary.from(d), hours);
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
