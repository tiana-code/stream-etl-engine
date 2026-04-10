package com.streametl.window;

import com.streametl.model.Record;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Getter
public final class TumblingWindow implements WindowFunction {

    private final Duration windowSize;

    private TumblingWindow(Duration windowSize) {
        this.windowSize = windowSize;
    }

    public static TumblingWindow of(Duration windowSize) {
        if (windowSize.isZero() || windowSize.isNegative()) {
            throw new IllegalArgumentException("Window size must be positive");
        }
        return new TumblingWindow(windowSize);
    }

    @Override
    public List<List<Record>> apply(List<Record> records) {
        if (records.isEmpty()) {
            return List.of();
        }

        List<Record> sorted = records.stream()
                .sorted(Comparator.comparing(record -> record.getMetadata().timestamp()))
                .toList();

        Instant windowStart = sorted.getFirst().getMetadata().timestamp();
        Instant windowEnd = windowStart.plus(windowSize);

        List<List<Record>> windows = new ArrayList<>();
        List<Record> current = new ArrayList<>();

        for (Record record : sorted) {
            Instant recordTimestamp = record.getMetadata().timestamp();
            while (!recordTimestamp.isBefore(windowEnd)) {
                if (!current.isEmpty()) {
                    windows.add(List.copyOf(current));
                    current.clear();
                }
                windowStart = windowEnd;
                windowEnd = windowStart.plus(windowSize);
            }
            current.add(record);
        }

        if (!current.isEmpty()) {
            windows.add(List.copyOf(current));
        }

        return windows;
    }

    @Override
    public String name() {
        return "TumblingWindow[" + windowSize + "]";
    }

}
