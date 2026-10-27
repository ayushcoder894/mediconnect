package com.mediconnect.seed;

import com.mediconnect.appointment.Appointment;
import com.mediconnect.appointment.AppointmentRepository;
import com.mediconnect.appointment.AppointmentStatus;
import com.mediconnect.common.AppProperties;
import com.mediconnect.doctor.Doctor;
import com.mediconnect.doctor.DoctorAvailability;
import com.mediconnect.doctor.DoctorRepository;
import com.mediconnect.doctor.DoctorService;
import com.mediconnect.hospital.HospitalRepository;
import com.mediconnect.lab.*;
import com.mediconnect.patient.Patient;
import com.mediconnect.patient.PatientRepository;
import com.mediconnect.prescription.Prescription;
import com.mediconnect.prescription.PrescriptionItem;
import com.mediconnect.prescription.PrescriptionRepository;
import com.mediconnect.review.Review;
import com.mediconnect.review.ReviewRepository;
import com.mediconnect.risk.RiskAssessment;
import com.mediconnect.risk.RiskAssessmentRepository;
import com.mediconnect.user.Role;
import com.mediconnect.user.User;
import com.mediconnect.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

/**
 * Fills an empty database with a realistic month of activity so every screen and
 * chart has data on first launch. Runs only when app.seed-demo-data=true and the
 * users table is empty. Uses a fixed random seed, so every run produces the same data.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    public static final String DEMO_PASSWORD = "Demo@1234";

    private final AppProperties props;
    private final UserRepository users;
    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final HospitalRepository hospitals;
    private final AppointmentRepository appointments;
    private final LabTestRepository labTests;
    private final LabBookingRepository labBookings;
    private final PrescriptionRepository prescriptions;
    private final RiskAssessmentRepository risks;
    private final ReviewRepository reviews;
    private final PasswordEncoder passwordEncoder;

    private final Random random = new Random(42);
    private final Set<String> takenDoctorSlots = new HashSet<>();
    private final Set<String> takenPatientSlots = new HashSet<>();

    private record DoctorSeed(String name, String email, String specialization, String qualification, int years,
                              int fee, long hospitalId, int slotMinutes, String hours, String bio) {
    }

    private record RxTemplate(String diagnosis, String[][] items) {
    }

    private static final List<DoctorSeed> DOCTORS = List.of(
            new DoctorSeed("Dr. Ananya Sharma", "ananya.sharma", "Cardiology", "MBBS, MD, DM (Cardiology)", 14, 900, 3, 30, "MON-FRI 09:00-13:00", "Interventional cardiologist focused on preventive heart care and hypertension management."),
            new DoctorSeed("Dr. Rohan Mehta", "rohan.mehta", "Endocrinology", "MBBS, MD, DM (Endocrinology)", 11, 800, 1, 30, "MON-SAT 10:00-14:00", "Diabetes and thyroid specialist. Runs the hospital's diabetes reversal clinic."),
            new DoctorSeed("Dr. Priya Nair", "priya.nair", "Dermatology", "MBBS, MD (Dermatology)", 8, 600, 2, 20, "MON-FRI 14:00-18:00", "Treats acne, eczema and hair loss with evidence-based skin care."),
            new DoctorSeed("Dr. Vikram Singh", "vikram.singh", "Orthopedics", "MBBS, MS (Orthopedics)", 16, 1000, 4, 30, "TUE-SAT 09:00-13:00", "Joint replacement and sports injury surgeon."),
            new DoctorSeed("Dr. Kavya Iyer", "kavya.iyer", "Pediatrics", "MBBS, MD (Pediatrics)", 9, 500, 1, 20, "MON-SAT 09:00-12:00", "Child health, vaccinations and growth monitoring."),
            new DoctorSeed("Dr. Arjun Kapoor", "arjun.kapoor", "Neurology", "MBBS, MD, DM (Neurology)", 12, 1100, 2, 30, "MON-FRI 11:00-15:00", "Headache, epilepsy and stroke specialist."),
            new DoctorSeed("Dr. Meera Joshi", "meera.joshi", "Gynecology", "MBBS, MS (Obstetrics & Gynaecology)", 13, 700, 6, 30, "MON-FRI 10:00-14:00", "Women's health, pregnancy care and PCOS management."),
            new DoctorSeed("Dr. Sameer Khan", "sameer.khan", "General Medicine", "MBBS, MD (Internal Medicine)", 7, 400, 1, 20, "MON-SAT 09:00-17:00", "First point of contact for fevers, infections and chronic disease follow-up."),
            new DoctorSeed("Dr. Neha Gupta", "neha.gupta", "Psychiatry", "MBBS, MD (Psychiatry)", 10, 900, 4, 45, "MON-FRI 15:00-19:00", "Anxiety, depression and sleep disorders, with therapy and medication."),
            new DoctorSeed("Dr. Aditya Rao", "aditya.rao", "Nephrology", "MBBS, MD, DM (Nephrology)", 15, 1200, 5, 30, "MON-FRI 09:00-13:00", "Kidney disease, dialysis and transplant care."),
            new DoctorSeed("Dr. Ishaan Verma", "ishaan.verma", "Ophthalmology", "MBBS, MS (Ophthalmology)", 6, 550, 2, 20, "TUE-SAT 10:00-14:00", "Cataract, glaucoma and vision correction."));

    private static final Map<String, RxTemplate> RX = Map.ofEntries(
            Map.entry("Cardiology", new RxTemplate("Essential hypertension", new String[][]{{"Amlodipine", "5 mg", "Once daily", "30", "Morning"}, {"Atorvastatin", "20 mg", "Once daily", "30", "At night"}})),
            Map.entry("Endocrinology", new RxTemplate("Type 2 diabetes mellitus", new String[][]{{"Metformin", "500 mg", "Twice daily", "30", "After meals"}, {"Glimepiride", "1 mg", "Once daily", "30", "Before breakfast"}})),
            Map.entry("Dermatology", new RxTemplate("Atopic dermatitis", new String[][]{{"Cetirizine", "10 mg", "Once daily", "10", "At night"}, {"Mometasone cream 0.1%", "Thin layer", "Twice daily", "14", "Affected areas only"}})),
            Map.entry("Orthopedics", new RxTemplate("Lumbar muscle strain", new String[][]{{"Aceclofenac", "100 mg", "Twice daily", "5", "After food"}, {"Thiocolchicoside", "4 mg", "Twice daily", "5", null}})),
            Map.entry("Pediatrics", new RxTemplate("Acute upper respiratory infection", new String[][]{{"Paracetamol syrup", "5 ml", "Three times daily", "3", "Only if fever"}, {"Saline nasal drops", "2 drops", "Three times daily", "5", null}})),
            Map.entry("Neurology", new RxTemplate("Migraine without aura", new String[][]{{"Naproxen", "500 mg", "As needed", "10", "Max 2 per day"}, {"Propranolol", "20 mg", "Twice daily", "30", null}})),
            Map.entry("Gynecology", new RxTemplate("Iron deficiency anaemia", new String[][]{{"Ferrous ascorbate", "100 mg", "Once daily", "60", "After lunch"}, {"Folic acid", "5 mg", "Once daily", "60", null}})),
            Map.entry("General Medicine", new RxTemplate("Viral fever", new String[][]{{"Paracetamol", "650 mg", "Three times daily", "5", "After food"}, {"ORS sachet", "1 sachet", "As needed", "3", "In 1 litre water"}})),
            Map.entry("Psychiatry", new RxTemplate("Generalised anxiety disorder", new String[][]{{"Escitalopram", "10 mg", "Once daily", "30", "Morning"}})),
            Map.entry("Nephrology", new RxTemplate("Chronic kidney disease, stage 2", new String[][]{{"Telmisartan", "40 mg", "Once daily", "30", "Morning"}, {"Sodium bicarbonate", "500 mg", "Twice daily", "30", null}})),
            Map.entry("Ophthalmology", new RxTemplate("Allergic conjunctivitis", new String[][]{{"Olopatadine 0.1% eye drops", "1 drop", "Twice daily", "14", "Both eyes"}})));

    private static final Map<String, String[]> REASONS = Map.ofEntries(
            Map.entry("Cardiology", new String[]{"Chest discomfort on exertion", "High blood pressure follow-up", "Palpitations"}),
            Map.entry("Endocrinology", new String[]{"Raised blood sugar in recent test", "Thyroid follow-up", "Diabetes review"}),
            Map.entry("Dermatology", new String[]{"Itchy skin rash", "Acne treatment", "Hair fall"}),
            Map.entry("Orthopedics", new String[]{"Lower back pain", "Knee pain while climbing stairs", "Shoulder injury"}),
            Map.entry("Pediatrics", new String[]{"Child has fever and cough", "Vaccination", "Growth check-up"}),
            Map.entry("Neurology", new String[]{"Frequent headaches", "Dizziness", "Numbness in hands"}),
            Map.entry("Gynecology", new String[]{"Irregular periods", "Pregnancy check-up", "PCOS follow-up"}),
            Map.entry("General Medicine", new String[]{"Fever for three days", "General health check-up", "Stomach pain"}),
            Map.entry("Psychiatry", new String[]{"Trouble sleeping", "Anxiety and stress", "Low mood"}),
            Map.entry("Nephrology", new String[]{"Raised creatinine", "Swelling in feet", "Kidney function follow-up"}),
            Map.entry("Ophthalmology", new String[]{"Blurred vision", "Red and itchy eyes", "Routine eye test"}));

    private static final String[] FIRST = {"Aarav", "Vivaan", "Aditi", "Diya", "Kabir", "Ira", "Reyansh", "Saanvi", "Arnav", "Myra", "Vihaan", "Anika", "Krish", "Pari", "Dhruv", "Riya", "Atharv", "Navya", "Yash", "Tara", "Om", "Kiara", "Laksh", "Siya", "Harsh", "Pooja", "Nikhil", "Sneha", "Manav", "Isha"};
    private static final String[] LAST = {"Sharma", "Verma", "Gupta", "Singh", "Patel", "Reddy", "Iyer", "Kapoor", "Malhotra", "Bansal", "Chopra", "Joshi", "Nair", "Rao", "Mishra", "Agarwal", "Saxena", "Bhatia", "Kulkarni", "Desai"};
    private static final String[] COMMENTS = {"Very patient and explained everything clearly.", "Diagnosis was spot on, feeling much better.", "Short waiting time and friendly staff.", "Listened carefully and did not rush.", "Good doctor, but the wait was a bit long.", "Helpful advice on diet and lifestyle.", "Highly recommended.", "Clear instructions and follow-up plan."};
    private static final String[] TAGS = {"Explains well", "On time", "Friendly", "Thorough", "Good follow-up"};
    private static final String[] SMOKING = {"never", "never", "never", "former", "current", "No Info", "not current"};

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!props.seedDemoData() || users.count() > 0) {
            return;
        }
        log.info("Seeding demo data...");
        String hash = passwordEncoder.encode(DEMO_PASSWORD);

        users.save(new User("admin@mediconnect.dev", hash, "Front Desk Admin", "+91 98100 00001", Role.ADMIN));
        List<Doctor> docs = DOCTORS.stream().map(s -> createDoctor(s, hash, true)).toList();
        createDoctor(new DoctorSeed("Dr. Tanvi Desai", "tanvi.desai", "ENT", "MBBS, MS (ENT)", 5, 600, 6, 30,
                "MON-FRI 10:00-13:00", "Ear, nose and throat surgeon."), hash, false);

        Patient demo = createDemoPatient(hash);
        List<Patient> pats = new ArrayList<>();
        pats.add(demo);
        for (int i = 0; i < 34; i++) {
            pats.add(createPatient(i, hash));
        }

        List<LabTest> routine = labTests.findAll().stream().filter(t -> !t.parameterList().isEmpty()).toList();
        seedAppointments(docs, pats);
        seedLabBookings(pats, routine);
        seedRisk(pats);
        seedDemoJourney(demo, docs, routine);
        log.info("Demo data ready. Log in with patient@mediconnect.dev / {}", DEMO_PASSWORD);
    }

    // ---- people ----

    private Doctor createDoctor(DoctorSeed s, String hash, boolean approved) {
        User u = users.save(new User(s.email() + "@mediconnect.dev", hash, s.name(),
                "+91 98" + (100 + random.nextInt(900)) + " " + (10000 + random.nextInt(90000)), Role.DOCTOR));
        Doctor d = new Doctor();
        d.setUser(u);
        d.setSpecialization(s.specialization());
        d.setQualification(s.qualification());
        d.setExperienceYears(s.years());
        d.setConsultationFee(BigDecimal.valueOf(s.fee()));
        d.setHospital(hospitals.findById(s.hospitalId()).orElseThrow());
        d.setSlotMinutes(s.slotMinutes());
        d.setBio(s.bio());
        d.setApproved(approved);
        // "MON-FRI 09:00-13:00"
        String[] parts = s.hours().split(" ");
        String[] days = parts[0].split("-");
        String[] times = parts[1].split("-");
        DayOfWeek from = dayOf(days[0]);
        DayOfWeek to = dayOf(days[1]);
        for (DayOfWeek day : EnumSet.range(from, to)) {
            d.getAvailability().add(new DoctorAvailability(d, day, LocalTime.parse(times[0]), LocalTime.parse(times[1])));
        }
        return doctors.save(d);
    }

    private static DayOfWeek dayOf(String abbrev) {
        return Arrays.stream(DayOfWeek.values()).filter(d -> d.name().startsWith(abbrev)).findFirst().orElseThrow();
    }

    private Patient createDemoPatient(String hash) {
        User u = users.save(new User("patient@mediconnect.dev", hash, "Rahul Verma", "+91 98765 43210", Role.PATIENT));
        Patient p = new Patient(u);
        p.setDateOfBirth(LocalDate.now().minusYears(46).minusDays(40));
        p.setGender("Male");
        p.setBloodGroup("B+");
        p.setHeightCm(new BigDecimal("172.0"));
        p.setWeightKg(new BigDecimal("84.0"));
        p.setAddress("House 21, Sector 7, Karnal, Haryana");
        p.setMedicalHistory("Hypertension diagnosed in 2021. Father has type 2 diabetes.");
        p.setAllergies("Penicillin");
        p.setSmokingHistory("former");
        p.setHypertension(true);
        return patients.save(p);
    }

    private Patient createPatient(int i, String hash) {
        String first = FIRST[i % FIRST.length];
        String last = LAST[(i * 7) % LAST.length];
        User u = users.save(new User(first.toLowerCase() + "." + last.toLowerCase() + i + "@example.com", hash,
                first + " " + last, "+91 9" + (100000000 + random.nextInt(899999999)), Role.PATIENT));
        Patient p = new Patient(u);
        p.setDateOfBirth(LocalDate.now().minusYears(18 + random.nextInt(55)).minusDays(random.nextInt(365)));
        p.setGender(i % 2 == 0 ? "Male" : "Female");
        p.setBloodGroup(new String[]{"A+", "B+", "O+", "AB+", "O-", "A-"}[random.nextInt(6)]);
        p.setHeightCm(BigDecimal.valueOf(150 + random.nextInt(35)));
        p.setWeightKg(BigDecimal.valueOf(48 + random.nextInt(45)));
        p.setSmokingHistory(SMOKING[random.nextInt(SMOKING.length)]);
        p.setHypertension(random.nextInt(5) == 0);
        p.setHeartDisease(random.nextInt(12) == 0);
        return patients.save(p);
    }

    // ---- activity ----

    private void seedAppointments(List<Doctor> docs, List<Patient> pats) {
        LocalDate today = LocalDate.now();
        for (int offset = -30; offset <= 7; offset++) {
            LocalDate day = today.plusDays(offset);
            int count = offset < 0 ? 5 + random.nextInt(6) : offset == 0 ? 8 : 2 + random.nextInt(4);
            for (int i = 0; i < count; i++) {
                Doctor d = docs.get(random.nextInt(docs.size()));
                Patient p = pats.get(1 + random.nextInt(pats.size() - 1));
                LocalDateTime slot = randomSlot(d, day);
                if (slot == null || !reserve(d, p, slot)) {
                    continue;
                }
                Appointment a = new Appointment(d, p, slot, pick(REASONS.get(d.getSpecialization())));
                a.setStatus(statusFor(slot));
                if (a.getStatus() == AppointmentStatus.CHECKED_IN || a.getStatus() == AppointmentStatus.COMPLETED) {
                    a.setCheckedInAt(slot.minusMinutes(5 + random.nextInt(10)));
                }
                appointments.save(a);
                if (a.getStatus() == AppointmentStatus.COMPLETED) {
                    if (random.nextInt(10) < 6) {
                        prescribe(a);
                    }
                    if (random.nextInt(10) < 6) {
                        review(a);
                    }
                }
            }
        }
    }

    private AppointmentStatus statusFor(LocalDateTime slot) {
        LocalDateTime now = LocalDateTime.now();
        int roll = random.nextInt(100);
        if (slot.toLocalDate().isBefore(now.toLocalDate())) {
            return roll < 78 ? AppointmentStatus.COMPLETED : roll < 90 ? AppointmentStatus.CANCELLED : AppointmentStatus.NO_SHOW;
        }
        if (slot.toLocalDate().equals(now.toLocalDate())) {
            if (slot.isBefore(now.minusHours(1))) {
                return roll < 85 ? AppointmentStatus.COMPLETED : AppointmentStatus.NO_SHOW;
            }
            if (slot.isBefore(now.plusHours(1))) {
                return AppointmentStatus.CHECKED_IN;
            }
            return roll < 50 ? AppointmentStatus.CONFIRMED : AppointmentStatus.BOOKED;
        }
        return roll < 35 ? AppointmentStatus.CONFIRMED : roll < 92 ? AppointmentStatus.BOOKED : AppointmentStatus.CANCELLED;
    }

    private LocalDateTime randomSlot(Doctor d, LocalDate day) {
        Optional<DoctorAvailability> hours = d.getAvailability().stream()
                .filter(a -> a.getDayOfWeek() == day.getDayOfWeek()).findFirst();
        if (hours.isEmpty()) {
            return null;
        }
        long minutes = Duration.between(hours.get().getStartTime(), hours.get().getEndTime()).toMinutes();
        int slots = (int) (minutes / d.getSlotMinutes());
        LocalDateTime slot = day.atTime(hours.get().getStartTime()).plusMinutes((long) random.nextInt(slots) * d.getSlotMinutes());
        return DoctorService.isValidSlot(d, slot) ? slot : null;
    }

    private boolean reserve(Doctor d, Patient p, LocalDateTime slot) {
        if (takenDoctorSlots.contains(d.getId() + "@" + slot) || takenPatientSlots.contains(p.getId() + "@" + slot)) {
            return false;
        }
        takenDoctorSlots.add(d.getId() + "@" + slot);
        takenPatientSlots.add(p.getId() + "@" + slot);
        return true;
    }

    private void prescribe(Appointment a) {
        RxTemplate t = RX.get(a.getDoctor().getSpecialization());
        Prescription rx = new Prescription();
        rx.setAppointment(a);
        rx.setDoctor(a.getDoctor());
        rx.setPatient(a.getPatient());
        rx.setDiagnosis(t.diagnosis());
        rx.setNotes("Review after the course. Stay hydrated and follow the diet chart.");
        rx.setFollowUpDate(a.getSlotStart().toLocalDate().plusDays(30));
        for (String[] i : t.items()) {
            PrescriptionItem item = new PrescriptionItem();
            item.setPrescription(rx);
            item.setMedicine(i[0]);
            item.setDosage(i[1]);
            item.setFrequency(i[2]);
            item.setDurationDays(Integer.parseInt(i[3]));
            item.setInstructions(i[4]);
            rx.getItems().add(item);
        }
        prescriptions.save(rx);
    }

    private void review(Appointment a) {
        int rating = new int[]{5, 5, 5, 4, 4, 4, 3}[random.nextInt(7)];
        a.setReviewed(true);
        a.getDoctor().addRating(rating);
        Review r = new Review();
        r.setDoctorId(a.getDoctor().getId());
        r.setPatientId(a.getPatient().getId());
        String[] name = a.getPatient().getUser().getFullName().split(" ");
        r.setPatientName(name[0] + " " + name[name.length - 1].charAt(0) + ".");
        r.setAppointmentId(a.getId());
        r.setRating(rating);
        r.setComment(pick(COMMENTS));
        r.setTags(List.of(pick(TAGS)));
        r.setCreatedAt(a.getSlotStart().plusHours(3).atZone(ZoneId.systemDefault()).toInstant());
        reviews.save(r);
    }

    private void seedLabBookings(List<Patient> pats, List<LabTest> routine) {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 70; i++) {
            Patient p = pats.get(1 + random.nextInt(pats.size() - 1));
            LabTest t = routine.get(random.nextInt(routine.size()));
            LocalDateTime when = today.plusDays(-28 + random.nextInt(32)).atTime(8 + random.nextInt(4), random.nextBoolean() ? 0 : 30);
            LabBooking b = newBooking(p, t, when);
            if (when.toLocalDate().isBefore(today.minusDays(1))) {
                fillResults(b, null);
            } else if (when.toLocalDate().isBefore(today.plusDays(1))) {
                b.setStatus(LabBooking.Status.SAMPLE_COLLECTED);
            }
            labBookings.save(b);
        }
    }

    private LabBooking newBooking(Patient p, LabTest t, LocalDateTime when) {
        LabBooking b = new LabBooking();
        b.setPatient(p);
        b.setLabTest(t);
        b.setScheduledAt(when);
        b.setPrice(t.getPrice());
        return b;
    }

    /** Random values around the reference range, or the given fixed values. */
    private void fillResults(LabBooking b, Map<String, String> fixed) {
        for (LabTest.Parameter param : b.getLabTest().parameterList()) {
            String value = fixed != null && fixed.containsKey(param.name()) ? fixed.get(param.name()) : randomValue(param.referenceRange());
            LabResult r = new LabResult();
            r.setBooking(b);
            r.setParameter(param.name());
            r.setValue(value);
            r.setUnit(param.unit());
            r.setReferenceRange(param.referenceRange());
            r.setFlag(LabResult.flagFor(value, param.referenceRange()));
            b.getResults().add(r);
        }
        b.setStatus(LabBooking.Status.REPORT_READY);
    }

    private String randomValue(String range) {
        String[] bounds = range.split("-");
        double low = Double.parseDouble(bounds[0]);
        double high = Double.parseDouble(bounds[1]);
        double floor = low == 0 ? high * 0.4 : low * 0.85;
        double value = floor + random.nextDouble() * (high * 1.25 - floor);
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).toPlainString();
    }

    private void seedRisk(List<Patient> pats) {
        String[] bands = {"LOW", "LOW", "LOW", "MODERATE", "MODERATE", "HIGH", "VERY_HIGH"};
        for (int i = 0; i < 42; i++) {
            Patient p = pats.get(1 + random.nextInt(pats.size() - 1));
            String band = bands[random.nextInt(bands.length)];
            double prob = switch (band) {
                case "LOW" -> 0.02 + random.nextDouble() * 0.17;
                case "MODERATE" -> 0.21 + random.nextDouble() * 0.28;
                case "HIGH" -> 0.51 + random.nextDouble() * 0.28;
                default -> 0.81 + random.nextDouble() * 0.17;
            };
            RiskAssessment r = new RiskAssessment();
            r.setPatient(p);
            r.setGender(p.getGender());
            r.setAge(p.age());
            r.setHypertension(p.isHypertension());
            r.setHeartDisease(p.isHeartDisease());
            r.setSmokingHistory(p.getSmokingHistory());
            r.setBmi(p.bmi());
            r.setHba1c(BigDecimal.valueOf(4.8 + prob * 4).setScale(1, RoundingMode.HALF_UP));
            r.setGlucose((int) (85 + prob * 150));
            r.setProbability(BigDecimal.valueOf(prob).setScale(4, RoundingMode.HALF_UP));
            r.setRiskBand(band);
            r.setModelVersion("lightgbm-1.0");
            r.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(30)).minusHours(random.nextInt(10)));
            risks.save(r);
        }
    }

    /**
     * The demo patient's story: raised sugar on a recent screening panel (ready to
     * feed the risk check), past visits with a prescription, and upcoming bookings.
     */
    private void seedDemoJourney(Patient demo, List<Doctor> docs, List<LabTest> routine) {
        LabTest panel = routine.stream().filter(t -> "DSP".equals(t.getCode())).findFirst().orElseThrow();
        LabTest lipid = routine.stream().filter(t -> "LIPID".equals(t.getCode())).findFirst().orElseThrow();
        LabTest cbc = routine.stream().filter(t -> "CBC".equals(t.getCode())).findFirst().orElseThrow();

        LabBooking screening = newBooking(demo, panel, LocalDate.now().minusDays(3).atTime(8, 30));
        fillResults(screening, Map.of("HbA1c", "6.4", "Blood Glucose", "132"));
        labBookings.save(screening);
        LabBooking blood = newBooking(demo, cbc, LocalDate.now().minusDays(20).atTime(9, 0));
        fillResults(blood, null);
        labBookings.save(blood);
        labBookings.save(newBooking(demo, lipid, LocalDate.now().plusDays(2).atTime(8, 0)));

        Doctor gp = byspecialization(docs, "General Medicine");
        Doctor cardio = byspecialization(docs, "Cardiology");
        seedDoctorDay(gp, LocalDate.now());
        Appointment pastGp = pastAppointment(demo, gp, 18, "Fever for three days");
        prescribe(pastGp);
        pastAppointment(demo, cardio, 9, "High blood pressure follow-up");

        for (int days = 1; days <= 6; days++) {
            LocalDateTime slot = randomSlot(gp, LocalDate.now().plusDays(days));
            if (slot != null && reserve(gp, demo, slot)) {
                Appointment upcoming = new Appointment(gp, demo, slot, "Discuss recent blood sugar report");
                upcoming.setStatus(AppointmentStatus.CONFIRMED);
                appointments.save(upcoming);
                break;
            }
        }
    }

    /** Gives the demo doctor a full, realistic clinic day so the schedule screen is never empty. */
    private void seedDoctorDay(Doctor d, LocalDate day) {
        List<Patient> pats = patients.findAll();
        Optional<DoctorAvailability> hours = d.getAvailability().stream()
                .filter(a -> a.getDayOfWeek() == day.getDayOfWeek()).findFirst();
        if (hours.isEmpty()) {
            return;
        }
        LocalDateTime slot = day.atTime(hours.get().getStartTime());
        LocalDateTime end = day.atTime(hours.get().getEndTime());
        int i = 0;
        while (!slot.plusMinutes(d.getSlotMinutes()).isAfter(end)) {
            Patient p = pats.get(1 + (i * 7) % (pats.size() - 1));
            if (i % 3 == 0 && reserve(d, p, slot)) {
                Appointment a = new Appointment(d, p, slot, pick(REASONS.get(d.getSpecialization())));
                a.setStatus(statusFor(slot));
                if (a.getStatus() == AppointmentStatus.CHECKED_IN || a.getStatus() == AppointmentStatus.COMPLETED) {
                    a.setCheckedInAt(slot.minusMinutes(6));
                }
                appointments.save(a);
            }
            slot = slot.plusMinutes(d.getSlotMinutes());
            i++;
        }
    }

    private Appointment pastAppointment(Patient p, Doctor d, int daysAgo, String reason) {
        for (int shift = 0; shift < 7; shift++) {
            LocalDateTime slot = randomSlot(d, LocalDate.now().minusDays(daysAgo + shift));
            if (slot != null && reserve(d, p, slot)) {
                Appointment a = new Appointment(d, p, slot, reason);
                a.setStatus(AppointmentStatus.COMPLETED);
                a.setCheckedInAt(slot.minusMinutes(8));
                return appointments.save(a);
            }
        }
        throw new IllegalStateException("No free slot for demo appointment");
    }

    private static Doctor byspecialization(List<Doctor> docs, String specialization) {
        return docs.stream().filter(d -> d.getSpecialization().equals(specialization)).findFirst().orElseThrow();
    }

    private <T> T pick(T[] values) {
        return values[random.nextInt(values.length)];
    }
}
