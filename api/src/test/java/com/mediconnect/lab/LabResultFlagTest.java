package com.mediconnect.lab;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class LabResultFlagTest {

    @ParameterizedTest
    @CsvSource({
            "5.2, 4.0-5.6, NORMAL",
            "6.4, 4.0-5.6, HIGH",
            "3.1, 4.0-5.6, LOW",
            "4.0, 4.0-5.6, NORMAL",
            "5.6, 4.0-5.6, NORMAL"})
    void flagsNumericValuesAgainstTheRange(String value, String range, String expected) {
        assertThat(LabResult.flagFor(value, range)).isEqualTo(expected);
    }

    @Test
    void noFlagForTextFindings() {
        assertThat(LabResult.flagFor("No pathogenic variant detected", null)).isNull();
        assertThat(LabResult.flagFor("Positive", "4.0-5.6")).isNull();
    }

    @Test
    void parsesTheResultTemplate() {
        LabTest t = new LabTest();
        t.setParameters("HbA1c|%|4.0-5.6;Blood Glucose|mg/dL|70-99");
        assertThat(t.parameterList()).containsExactly(
                new LabTest.Parameter("HbA1c", "%", "4.0-5.6"),
                new LabTest.Parameter("Blood Glucose", "mg/dL", "70-99"));
    }
}
