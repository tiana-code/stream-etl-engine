package com.streametl.model;

import java.util.Arrays;

public enum DataLakeLayer {
    BRONZE,
    SILVER,
    GOLD;

    public static DataLakeLayer from(String value) {
        return Arrays.stream(values())
                .filter(layer -> layer.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported targetLayer value: " + value));
    }

    public boolean canPromoteTo(DataLakeLayer target) {
        return target.ordinal() == this.ordinal() + 1;
    }
}
