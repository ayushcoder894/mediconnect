package com.mediconnect.lab;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "lab_results")
@Getter
@Setter
@NoArgsConstructor
public class LabResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id")
    private LabBooking booking;

    private String parameter;
    private String value;
    private String unit;

    @Column(name = "reference_range")
    private String referenceRange;

    private String flag;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * NORMAL, LOW or HIGH for a numeric value against a "low-high" range; null when
     * either side is not numeric (e.g. a written genetic finding).
     */
    public static String flagFor(String value, String range) {
        if (value == null || range == null || !range.contains("-")) {
            return null;
        }
        try {
            BigDecimal v = new BigDecimal(value.trim());
            String[] bounds = range.split("-", 2);
            BigDecimal low = new BigDecimal(bounds[0].trim());
            BigDecimal high = new BigDecimal(bounds[1].trim());
            if (v.compareTo(low) < 0) {
                return "LOW";
            }
            return v.compareTo(high) > 0 ? "HIGH" : "NORMAL";
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
