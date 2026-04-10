package com.streametl.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String error,
        String message,
        String sourceId
) {
    public ErrorResponse(String error, String message) {
        this(error, message, null);
    }
}
