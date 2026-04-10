package com.streametl.delta;

public record DeltaResult(
        ChangeType changeType,
        String field,
        Object oldValue,
        Object newValue
) {

    public static DeltaResult added(String field, Object newValue) {
        return new DeltaResult(ChangeType.ADDED, field, null, newValue);
    }

    public static DeltaResult modified(String field, Object oldValue, Object newValue) {
        return new DeltaResult(ChangeType.MODIFIED, field, oldValue, newValue);
    }

    public static DeltaResult deleted(String field, Object oldValue) {
        return new DeltaResult(ChangeType.DELETED, field, oldValue, null);
    }
}
