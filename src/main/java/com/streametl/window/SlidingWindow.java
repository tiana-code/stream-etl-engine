package com.streametl.window;

import com.streametl.model.Record;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Getter
public final class SlidingWindow implements WindowFunction {

    private final Duration windowSize;
    private final Duration slideInterval;

    private SlidingWindow(Duration windowSize, Duration slideInterval) {
        this.windowSize = windowSize;
        this.slideInterval = slideInterval;
    }

    public static SlidingWindow of(Duration windowSize, Duration slideInterval) {
        if (windowSize.isZero() || windowSize.isNegative()) {
            throw new IllegalArgumentException("Window size must be positive");
        }
        if (slideInterval.isZero() || slideInterval.isNegative()) {
            throw new IllegalArgumentException("Slide interval must be positive");
        }
        if (slideInterval.compareTo(windowSize) > 0) {
            throw new IllegalArgumentException("Slide interval must not exceed window size");
        }
        return new SlidingWindow(windowSize, slideInterval);
    }

    @Override
    public List<List<Record>> apply(List<Record> records) {
        if (records.isEmpty()) {
            return List.of();
        }

        List<Record> sorted = records.stream()
                .sorted(Comparator.comparing(record -> record.getMetadata().timestamp()))
                .toList();

        Instant firstTimestamp = sorted.getFirst().getMetadata().timestamp();
        Instant lastTimestamp = sorted.get(sorted.size() - 1).getMetadata().timestamp();

        List<List<Record>> windows = new ArrayList<>();
        Instant windowStart = firstTimestamp;

        while (!windowStart.isAfter(lastTimestamp)) {
            Instant windowEnd = windowStart.plus(windowSize);
            List<Record> window = new ArrayList<>();

            for (Record record : sorted) {
                Instant recordTimestamp = record.getMetadata().timestamp();
                if (!recordTimestamp.isBefore(windowStart) && recordTimestamp.isBefore(windowEnd)) {
                    window.add(record);
                }
            }

            if (!window.isEmpty()) {
                windows.add(List.copyOf(window));
            }

            windowStart = windowStart.plus(slideInterval);
        }

        return windows;
    }

    @Override
    public String name() {
        return "SlidingWindow[size=" + windowSize + ",slide=" + slideInterval + "]";
    }

}
