package com.mediconnect.lab;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "lab_tests")
@Getter
@Setter
@NoArgsConstructor
public class LabTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;
    private String name;
    private String category;
    private String description;
    private BigDecimal price;

    @Column(name = "partner_price")
    private BigDecimal partnerPrice;

    @Column(name = "sample_type")
    private String sampleType;

    @Column(name = "turnaround_hours")
    private int turnaroundHours;

    /** Result template, "name|unit|low-high;..." */
    private String parameters;

    private boolean active = true;

    public record Parameter(String name, String unit, String referenceRange) {}

    public List<Parameter> parameterList() {
        if (parameters == null || parameters.isBlank()) {
            return List.of();
        }
        return Arrays.stream(parameters.split(";"))
                .map(p -> p.split("\\|", -1))
                .map(p -> new Parameter(p[0].trim(), p.length > 1 ? p[1].trim() : "", p.length > 2 ? p[2].trim() : ""))
                .toList();
    }
}
