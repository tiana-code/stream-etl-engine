package com.streametl.pipeline.impl;

import com.streametl.model.Record;
import com.streametl.pipeline.PipelineStage;
import com.streametl.pipeline.PipelineValidationException;

import java.util.List;
import java.util.Set;

public class ValidationStage implements PipelineStage {

    private final Set<String> requiredFields;
    private final boolean failOnMissing;

    public ValidationStage(Set<String> requiredFields, boolean failOnMissing) {
        this.requiredFields = Set.copyOf(requiredFields);
        this.failOnMissing = failOnMissing;
    }

    public static ValidationStage requiring(String... fields) {
        return new ValidationStage(Set.of(fields), true);
    }

    public static ValidationStage soft(String... fields) {
        return new ValidationStage(Set.of(fields), false);
    }

    @Override
    public Record process(Record record) {
        List<String> missing = requiredFields.stream()
                .filter(fieldName -> !record.has(fieldName) || record.get(fieldName) == null)
                .sorted()
                .toList();

        if (!missing.isEmpty() && failOnMissing) {
            throw new PipelineValidationException(
                    "Record missing required fields: " + missing,
                    record.getMetadata().sourceId());
        }

        return record.withField("_validationPassed", missing.isEmpty())
                .withField("_missingFields", missing.isEmpty() ? List.of() : missing);
    }
}
