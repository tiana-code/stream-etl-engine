package com.streametl.model;

import java.time.Instant;

public record RecordMetadata(
        String sourceId,
        Instant timestamp,
        DataLakeLayer layer,
        int version
) {

    public static RecordMetadata initial(String sourceId) {
        return new RecordMetadata(sourceId, Instant.now(), DataLakeLayer.BRONZE, 1);
    }

    public RecordMetadata withLayer(DataLakeLayer newLayer) {
        return new RecordMetadata(sourceId, timestamp, newLayer, version + 1);
    }

    public RecordMetadata withVersion(int newVersion) {
        return new RecordMetadata(sourceId, timestamp, layer, newVersion);
    }
}
