package com.streametl.violation;

import com.streametl.model.Record;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ViolationDetectorTest {

    private ViolationDetector detector;

    @BeforeEach
    void setUp() {
        detector = new ViolationDetector();
    }

    @Test
    void detectsViolationWhenValueExceedsThreshold() {
        ViolationRule rule = new ViolationRule("price-ceiling", "price",
                Operator.GREATER_THAN, new BigDecimal("999.99"), ViolationSeverity.CRITICAL);

        Record record = Record.of(Map.of("id", "1", "price", new BigDecimal("1500.00")), "test");

        List<Violation> violations = detector.detect(record, List.of(rule));

        assertThat(violations).hasSize(1);
        assertThat(violations.getFirst().ruleId()).isEqualTo("price-ceiling");
        assertThat(violations.getFirst().severity()).isEqualTo(ViolationSeverity.CRITICAL);
    }

    @Test
    void detectsNoViolationWhenValueBelowThreshold() {
        ViolationRule rule = new ViolationRule("price-ceiling", "price",
                Operator.GREATER_THAN, new BigDecimal("999.99"), ViolationSeverity.CRITICAL);

        Record record = Record.of(Map.of("id", "1", "price", new BigDecimal("500.00")), "test");

        List<Violation> violations = detector.detect(record, List.of(rule));

        assertThat(violations).isEmpty();
    }

    @Test
    void skipsFieldWhenNotPresentInRecord() {
        ViolationRule rule = new ViolationRule("qty-check", "quantity",
                Operator.LESS_THAN, BigDecimal.ZERO, ViolationSeverity.WARNING);

        Record record = Record.of(Map.of("id", "1", "price", 100), "test");

        List<Violation> violations = detector.detect(record, List.of(rule));

        assertThat(violations).isEmpty();
    }

    @Test
    void detectsViolationFromStringNumericValue() {
        ViolationRule rule = new ViolationRule("qty-min", "qty",
                Operator.LESS_THAN, BigDecimal.ONE, ViolationSeverity.WARNING);

        Record record = Record.of(Map.of("qty", "0"), "test");

        List<Violation> violations = detector.detect(record, List.of(rule));

        assertThat(violations).hasSize(1);
    }

    @Test
    void detectsMultipleViolationsFromMultipleRules() {
        List<ViolationRule> rules = List.of(
                new ViolationRule("price-neg", "price",
                        Operator.LESS_THAN, BigDecimal.ZERO, ViolationSeverity.CRITICAL),
                new ViolationRule("qty-neg", "quantity",
                        Operator.LESS_THAN, BigDecimal.ZERO, ViolationSeverity.CRITICAL)
        );

        Record record = Record.of(Map.of("price", new BigDecimal("-10"), "quantity", new BigDecimal("-5")), "test");

        List<Violation> violations = detector.detect(record, rules);

        assertThat(violations).hasSize(2);
    }

    @Test
    void detectAboveSeverityFiltersCorrectly() {
        List<ViolationRule> rules = List.of(
                new ViolationRule("info-rule", "score",
                        Operator.LESS_THAN, new BigDecimal("50"), ViolationSeverity.INFO),
                new ViolationRule("critical-rule", "price",
                        Operator.GREATER_THAN, new BigDecimal("100"), ViolationSeverity.CRITICAL)
        );

        Record record = Record.of(Map.of("score", new BigDecimal("10"), "price", new BigDecimal("200")), "test");

        List<Violation> criticalOnly = detector.detectAboveSeverity(record, rules, ViolationSeverity.CRITICAL);

        assertThat(criticalOnly).hasSize(1);
        assertThat(criticalOnly.getFirst().severity()).isEqualTo(ViolationSeverity.CRITICAL);
    }

    @Test
    void detectBatchAggregatesViolationsFromAllRecords() {
        ViolationRule rule = new ViolationRule("price-neg", "price",
                Operator.LESS_THAN, BigDecimal.ZERO, ViolationSeverity.WARNING);

        List<Record> records = List.of(
                Record.of(Map.of("price", new BigDecimal("-1")), "src"),
                Record.of(Map.of("price", new BigDecimal("100")), "src"),
                Record.of(Map.of("price", new BigDecimal("-5")), "src")
        );

        List<Violation> violations = detector.detectBatch(records, List.of(rule));

        assertThat(violations).hasSize(2);
    }

    @Test
    void operatorEqualsMatchesExactValue() {
        ViolationRule rule = new ViolationRule("status-zero", "status",
                Operator.EQUALS, BigDecimal.ZERO, ViolationSeverity.INFO);

        Record record = Record.of(Map.of("status", BigDecimal.ZERO), "test");

        assertThat(detector.detect(record, List.of(rule))).hasSize(1);
    }

    @Test
    void violationContainsFieldAndActualValue() {
        ViolationRule rule = new ViolationRule("r1", "val",
                Operator.GREATER_THAN, BigDecimal.TEN, ViolationSeverity.WARNING);

        Record record = Record.of(Map.of("val", new BigDecimal("20"), "id", "abc"), "test");

        Violation violation = detector.detect(record, List.of(rule)).get(0);

        assertThat(violation.field()).isEqualTo("val");
        assertThat(violation.actualValue()).isEqualByComparingTo(new BigDecimal("20"));
        assertThat(violation.sourceRecord().get("id")).isEqualTo("abc");
        assertThat(violation.detectedAt()).isNotNull();
    }
}
