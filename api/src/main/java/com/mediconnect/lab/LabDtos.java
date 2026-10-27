package com.mediconnect.lab;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class LabDtos {

    private LabDtos() {
    }

    public record LabTestDto(Long id, String code, String name, String category, String description,
                             BigDecimal price, BigDecimal partnerPrice, String sampleType, int turnaroundHours,
                             List<LabTest.Parameter> parameters, boolean active) {

        public static LabTestDto from(LabTest t) {
            return new LabTestDto(t.getId(), t.getCode(), t.getName(), t.getCategory(), t.getDescription(),
                    t.getPrice(), t.getPartnerPrice(), t.getSampleType(), t.getTurnaroundHours(),
                    t.parameterList(), t.isActive());
        }
    }

    public record LabResultDto(String parameter, String value, String unit, String referenceRange, String flag) {

        public static LabResultDto from(LabResult r) {
            return new LabResultDto(r.getParameter(), r.getValue(), r.getUnit(), r.getReferenceRange(), r.getFlag());
        }
    }

    public record LabBookingDto(Long id, Long testId, String testName, String category, Long patientId,
                                String patientName, String orderedBy, LocalDateTime scheduledAt,
                                LabBooking.Status status, BigDecimal price, List<LabTest.Parameter> parameters,
                                List<LabResultDto> results) {

        public static LabBookingDto from(LabBooking b) {
            var t = b.getLabTest();
            return new LabBookingDto(b.getId(), t.getId(), t.getName(), t.getCategory(), b.getPatient().getId(),
                    b.getPatient().getUser().getFullName(),
                    b.getOrderedBy() == null ? null : b.getOrderedBy().getUser().getFullName(),
                    b.getScheduledAt(), b.getStatus(), b.getPrice(), t.parameterList(),
                    b.getResults().stream().map(LabResultDto::from).toList());
        }
    }

    public record BookLabRequest(@NotNull Long testId, @NotNull @Future LocalDateTime scheduledAt) {
    }

    public record DoctorOrderRequest(@NotNull Long appointmentId, @NotNull Long testId) {
    }

    public record StatusRequest(@NotNull LabBooking.Status status) {
    }

    /** Parameter name to value; for tests without a template use the key "Finding". */
    public record ResultsRequest(@NotEmpty Map<@NotBlank String, @NotBlank @Size(max = 255) String> values) {
    }

    public record UpsertLabTestRequest(
            @NotBlank @Size(max = 40) String code,
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Size(max = 40) String category,
            @Size(max = 500) String description,
            @NotNull @DecimalMin("0") BigDecimal price,
            @DecimalMin("0") BigDecimal partnerPrice,
            @Size(max = 40) String sampleType,
            @Min(1) @Max(2000) int turnaroundHours,
            @Size(max = 600) String parameters,
            boolean active) {
    }
}
