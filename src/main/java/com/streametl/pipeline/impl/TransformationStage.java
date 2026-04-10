package com.streametl.pipeline.impl;

import com.streametl.model.Record;
import com.streametl.pipeline.PipelineStage;

import com.streametl.util.DecimalConverter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class TransformationStage implements PipelineStage {

    private final Map<String, String> fieldMappings;
    private final Map<String, Function<Object, Object>> typeConverters;

    public TransformationStage(Map<String, String> fieldMappings, Map<String, Function<Object, Object>> typeConverters) {
        this.fieldMappings = Map.copyOf(fieldMappings);
        this.typeConverters = Map.copyOf(typeConverters);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Record process(Record record) {
        Record result = record;

        for (Map.Entry<String, String> mapping : fieldMappings.entrySet()) {
            String sourceField = mapping.getKey();
            String targetField = mapping.getValue();

            if (record.has(sourceField) && !sourceField.equals(targetField)) {
                result = result.withField(targetField, record.get(sourceField))
                        .withoutField(sourceField);
            }
        }

        for (Map.Entry<String, Function<Object, Object>> converter : typeConverters.entrySet()) {
            String field = converter.getKey();
            if (result.has(field)) {
                Object converted = converter.getValue().apply(result.get(field));
                result = result.withField(field, converted);
            }
        }

        return result;
    }

    public static class Builder {

        private final Map<String, String> fieldMappings = new LinkedHashMap<>();
        private final Map<String, Function<Object, Object>> typeConverters = new LinkedHashMap<>();

        public Builder rename(String from, String to) {
            fieldMappings.put(from, to);
            return this;
        }

        public Builder convert(String field, Function<Object, Object> converter) {
            typeConverters.put(field, converter);
            return this;
        }

        public Builder toDecimal(String field) {
            return convert(field, value -> {
                var result = DecimalConverter.toDecimal(value);
                return result != null ? result : value;
            });
        }

        public Builder toLowerCase(String field) {
            return convert(field, value -> value instanceof String str ? str.toLowerCase() : value);
        }

        public Builder trim(String field) {
            return convert(field, value -> value instanceof String str ? str.trim() : value);
        }

        public TransformationStage build() {
            return new TransformationStage(fieldMappings, typeConverters);
        }
    }
}
