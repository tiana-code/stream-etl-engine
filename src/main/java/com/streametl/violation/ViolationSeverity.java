package com.streametl.violation;

import java.util.Arrays;

public enum ViolationSeverity {
    INFO,
    WARNING,
    CRITICAL;

    public static ViolationSeverity from(String value) {
        return Arrays.stream(values())
                .filter(severity -> severity.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported severity value: " + value));
    }

    public boolean isAtLeast(ViolationSeverity other) {
        return this.ordinal() >= other.ordinal();
    }
}
