package com.streametl.pipeline.impl;

import com.streametl.model.Record;
import com.streametl.pipeline.PipelineStage;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.function.Function;

@RequiredArgsConstructor
public class EnrichmentStage implements PipelineStage {

    private final String lookupField;
    private final Function<Object, Map<String, Object>> referenceResolver;

    public static EnrichmentStage byField(String field, Function<Object, Map<String, Object>> resolver) {
        return new EnrichmentStage(field, resolver);
    }

    public static EnrichmentStage fromMap(String field, Map<Object, Map<String, Object>> referenceData) {
        return new EnrichmentStage(field, referenceData::get);
    }

    @Override
    public Record process(Record record) {
        if (!record.has(lookupField)) {
            return record;
        }

        Object lookupValue = record.get(lookupField);
        Map<String, Object> enrichmentData = referenceResolver.apply(lookupValue);

        if (enrichmentData == null || enrichmentData.isEmpty()) {
            return record;
        }

        Record enriched = record;
        for (Map.Entry<String, Object> entry : enrichmentData.entrySet()) {
            if (!enriched.has(entry.getKey())) {
                enriched = enriched.withField(entry.getKey(), entry.getValue());
            }
        }

        return enriched;
    }
}
