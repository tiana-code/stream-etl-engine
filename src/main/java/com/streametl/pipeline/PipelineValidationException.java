package com.streametl.pipeline;

import lombok.Getter;

@Getter
public class PipelineValidationException extends RuntimeException {

    private final String sourceId;

    public PipelineValidationException(String message, String sourceId) {
        super(message);
        this.sourceId = sourceId;
    }
}
