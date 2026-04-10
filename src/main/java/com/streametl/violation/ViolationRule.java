package com.streametl.violation;

import java.math.BigDecimal;

public record ViolationRule(
        String ruleId,
        String field,
        Operator operator,
        BigDecimal threshold,
        ViolationSeverity severity
) {

    public boolean matches(BigDecimal value) {
        int comparisonResult = value.compareTo(threshold);
        return switch (operator) {
            case GREATER_THAN -> comparisonResult > 0;
            case LESS_THAN -> comparisonResult < 0;
            case EQUALS -> comparisonResult == 0;
            case NOT_EQUALS -> comparisonResult != 0;
            case GREATER_THAN_OR_EQUAL -> comparisonResult >= 0;
            case LESS_THAN_OR_EQUAL -> comparisonResult <= 0;
        };
    }
}
