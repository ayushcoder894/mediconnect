package com.mediconnect.risk;

import com.mediconnect.audit.AuditService;
import com.mediconnect.lab.LabResult;
import com.mediconnect.lab.LabResultRepository;
import com.mediconnect.patient.Patient;
import com.mediconnect.patient.PatientService;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RiskService {

    static final String HBA1C = "HbA1c";
    static final String GLUCOSE = "Blood Glucose";

    private final AiClient ai;
    private final PatientService patientService;
    private final LabResultRepository labResults;
    private final RiskAssessmentRepository assessments;
    private final AuditService audit;

    public record LabValue(String value, LocalDateTime measuredAt) {
    }

    /** What the form can fill in automatically from the profile and latest lab results. */
    public record PrefillDto(String gender, Integer age, boolean hypertension, boolean heartDisease,
                             String smokingHistory, BigDecimal bmi, LabValue hba1c, LabValue glucose,
                             List<String> missing) {
    }

    public record AssessRequest(
            @NotNull @Pattern(regexp = "Male|Female|Other") String gender,
            @NotNull @Min(1) @Max(110) Integer age,
            boolean hypertension,
            boolean heartDisease,
            @NotNull @Pattern(regexp = "never|former|current|not current|ever|No Info") String smokingHistory,
            @NotNull @DecimalMin("10") @DecimalMax("80") BigDecimal bmi,
            @NotNull @DecimalMin("3") @DecimalMax("15") BigDecimal hba1c,
            @NotNull @Min(50) @Max(400) Integer glucose) {
    }

    public record RiskResultDto(Long id, double probability, String riskBand, String modelVersion,
                                List<String> factors, String recommendation, String suggestedSpecialization,
                                LocalDateTime createdAt, BigDecimal bmi, BigDecimal hba1c, int glucose) {
    }

    @Transactional(readOnly = true)
    public PrefillDto prefill() {
        Patient p = patientService.currentPatient();
        Optional<LabValue> hba1c = latest(p.getId(), HBA1C);
        Optional<LabValue> glucose = latest(p.getId(), GLUCOSE);
        List<String> missing = new ArrayList<>();
        if (p.getGender() == null) missing.add("gender");
        if (p.age() == null) missing.add("age");
        if (p.bmi() == null) missing.add("bmi");
        if (hba1c.isEmpty()) missing.add("hba1c");
        if (glucose.isEmpty()) missing.add("glucose");
        return new PrefillDto(p.getGender(), p.age(), p.isHypertension(), p.isHeartDisease(),
                p.getSmokingHistory() == null ? "No Info" : p.getSmokingHistory(), p.bmi(),
                hba1c.orElse(null), glucose.orElse(null), missing);
    }

    private Optional<LabValue> latest(Long patientId, String parameter) {
        return labResults.findLatest(patientId, parameter, PageRequest.of(0, 1)).stream().findFirst()
                .map((LabResult r) -> new LabValue(r.getValue(), r.getBooking().getScheduledAt()));
    }

    @Transactional
    public RiskResultDto assess(AssessRequest req) {
        Patient p = patientService.currentPatient();
        var prediction = ai.predictDiabetes(new AiClient.DiabetesInput(req.gender(), req.age(),
                req.hypertension() ? 1 : 0, req.heartDisease() ? 1 : 0, req.smokingHistory(),
                req.bmi().doubleValue(), req.hba1c().doubleValue(), req.glucose()));

        RiskAssessment r = new RiskAssessment();
        r.setPatient(p);
        r.setGender(req.gender());
        r.setAge(req.age());
        r.setHypertension(req.hypertension());
        r.setHeartDisease(req.heartDisease());
        r.setSmokingHistory(req.smokingHistory());
        r.setBmi(req.bmi());
        r.setHba1c(req.hba1c());
        r.setGlucose(req.glucose());
        r.setProbability(BigDecimal.valueOf(prediction.probability()).setScale(4, RoundingMode.HALF_UP));
        r.setRiskBand(prediction.riskBand());
        r.setModelVersion(prediction.modelVersion());
        assessments.save(r);
        audit.record("DIABETES_RISK_CHECK", "RiskAssessment", r.getId(), prediction.riskBand());
        return toDto(r, prediction.factors(), prediction.recommendation());
    }

    @Transactional(readOnly = true)
    public List<RiskResultDto> history() {
        Patient p = patientService.currentPatient();
        return assessments.findByPatientIdOrderByCreatedAtDesc(p.getId()).stream()
                .map(r -> toDto(r, List.of(), null)).toList();
    }

    static String suggestedSpecialization(String band) {
        return ("HIGH".equals(band) || "VERY_HIGH".equals(band)) ? "Endocrinology" : null;
    }

    private RiskResultDto toDto(RiskAssessment r, List<String> factors, String recommendation) {
        return new RiskResultDto(r.getId(), r.getProbability().doubleValue(), r.getRiskBand(), r.getModelVersion(),
                factors, recommendation, suggestedSpecialization(r.getRiskBand()), r.getCreatedAt(), r.getBmi(),
                r.getHba1c(), r.getGlucose());
    }
}
