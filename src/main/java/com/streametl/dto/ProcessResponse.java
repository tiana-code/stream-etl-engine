package com.streametl.dto;

import com.streametl.model.DataLakeLayer;

import java.util.Map;

public record ProcessResponse(
        Map<String, Object> fields,
        DataLakeLayer layer,
        String sourceId,
        int version
) {
}
