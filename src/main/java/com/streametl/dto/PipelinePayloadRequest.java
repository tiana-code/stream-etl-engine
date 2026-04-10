package com.streametl.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record PipelinePayloadRequest(
        @NotEmpty(message = "Payload must not be empty")
        Map<String, Object> payload
) {
}
