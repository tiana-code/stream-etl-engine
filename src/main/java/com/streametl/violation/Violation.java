package com.streametl.violation;

import com.streametl.model.Record;

import java.math.BigDecimal;
import java.time.Instant;

public record Violation(
        String ruleId,
        String field,
        BigDecimal actualValue,
        Record sourceRecord,
        Instant detectedAt,
        ViolationSeverity severity
) {

    public static Violation of(ViolationRule rule, Record record, BigDecimal actualValue) {
        return new Violation(
                rule.ruleId(),
                rule.field(),
                actualValue,
                record,
                Instant.now(),
                rule.severity());
    }
}
