package com.streametl.model;

import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Record {

    private final Map<String, Object> fields;
    @Getter
    private final RecordMetadata metadata;

    public Record(Map<String, Object> fields, RecordMetadata metadata) {
        this.fields = new HashMap<>(fields);
        this.metadata = Objects.requireNonNull(metadata);
    }

    public static Record of(Map<String, Object> fields, String sourceId) {
        return new Record(fields, RecordMetadata.initial(sourceId));
    }

    public Object get(String field) {
        return fields.get(field);
    }

    public boolean has(String field) {
        return fields.containsKey(field);
    }

    public Map<String, Object> getFields() {
        return Collections.unmodifiableMap(fields);
    }

    public Record withField(String field, Object value) {
        Map<String, Object> updated = new HashMap<>(fields);
        updated.put(field, value);
        return new Record(updated, metadata);
    }

    public Record withMetadata(RecordMetadata newMetadata) {
        return new Record(fields, newMetadata);
    }

    public Record withoutField(String field) {
        Map<String, Object> updated = new HashMap<>(fields);
        updated.remove(field);
        return new Record(updated, metadata);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Record other)) return false;
        return fields.equals(other.fields) && metadata.equals(other.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fields, metadata);
    }
}
