package com.streametl.dto;

import com.streametl.violation.ViolationSeverity;

import java.math.BigDecimal;
import java.time.Instant;

public record ViolationResponse(
        String ruleId,
        String field,
        BigDecimal actualValue,
        ViolationSeverity severity,
        Instant detectedAt
) {
}
