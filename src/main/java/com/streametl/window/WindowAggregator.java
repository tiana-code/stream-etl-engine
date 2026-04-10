package com.streametl.window;

import com.streametl.model.Record;
import com.streametl.util.DecimalConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class WindowAggregator {

    public long count(List<Record> window) {
        return window.size();
    }

    public Optional<BigDecimal> sum(List<Record> window, String field) {
        return window.stream()
                .map(record -> DecimalConverter.toDecimal(record.get(field)))
                .filter(Objects::nonNull)
                .reduce(BigDecimal::add);
    }

    public Optional<BigDecimal> average(List<Record> window, String field) {
        List<BigDecimal> values = window.stream()
                .map(record -> DecimalConverter.toDecimal(record.get(field)))
                .filter(Objects::nonNull)
                .toList();

        if (values.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal total = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return Optional.of(total.divide(BigDecimal.valueOf(values.size()), 10, RoundingMode.HALF_UP));
    }

    public Optional<BigDecimal> min(List<Record> window, String field) {
        return window.stream()
                .map(record -> DecimalConverter.toDecimal(record.get(field)))
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo);
    }

    public Optional<BigDecimal> max(List<Record> window, String field) {
        return window.stream()
                .map(record -> DecimalConverter.toDecimal(record.get(field)))
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo);
    }

    public AggregateStats stats(List<Record> window, String field) {
        return new AggregateStats(
                count(window),
                sum(window, field).orElse(null),
                average(window, field).orElse(null),
                min(window, field).orElse(null),
                max(window, field).orElse(null)
        );
    }

    public List<AggregateStats> aggregateWindows(List<List<Record>> windows, String field) {
        return windows.stream()
                .map(window -> stats(window, field))
                .toList();
    }

    public record AggregateStats(
            long count,
            BigDecimal sum,
            BigDecimal average,
            BigDecimal min,
            BigDecimal max
    ) {
    }
}
