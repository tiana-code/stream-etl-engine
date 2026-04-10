package com.streametl.violation;

import com.streametl.model.Record;
import com.streametl.util.DecimalConverter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ViolationDetector {

    public List<Violation> detect(Record record, List<ViolationRule> rules) {
        List<Violation> violations = new ArrayList<>();

        for (ViolationRule rule : rules) {
            if (!record.has(rule.field())) {
                continue;
            }

            BigDecimal value = DecimalConverter.toDecimal(record.get(rule.field()));
            if (value == null) {
                continue;
            }

            if (rule.matches(value)) {
                violations.add(Violation.of(rule, record, value));
            }
        }

        return violations;
    }

    public List<Violation> detectAboveSeverity(Record record, List<ViolationRule> rules, ViolationSeverity minimumSeverity) {
        return detect(record, rules).stream()
                .filter(violation -> violation.severity().isAtLeast(minimumSeverity))
                .toList();
    }

    public List<Violation> detectBatch(List<Record> records, List<ViolationRule> rules) {
        List<Violation> all = new ArrayList<>();
        for (Record record : records) {
            all.addAll(detect(record, rules));
        }
        return all;
    }
}
