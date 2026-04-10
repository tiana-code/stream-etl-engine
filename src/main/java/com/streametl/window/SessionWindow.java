package com.streametl.window;

import com.streametl.model.Record;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Getter
public final class SessionWindow implements WindowFunction {

    private final Duration gapTimeout;

    private SessionWindow(Duration gapTimeout) {
        this.gapTimeout = gapTimeout;
    }

    public static SessionWindow withGap(Duration gapTimeout) {
        if (gapTimeout.isZero() || gapTimeout.isNegative()) {
            throw new IllegalArgumentException("Gap timeout must be positive");
        }
        return new SessionWindow(gapTimeout);
    }

    @Override
    public List<List<Record>> apply(List<Record> records) {
        if (records.isEmpty()) {
            return List.of();
        }

        List<Record> sorted = records.stream()
                .sorted(Comparator.comparing(record -> record.getMetadata().timestamp()))
                .toList();

        List<List<Record>> sessions = new ArrayList<>();
        List<Record> current = new ArrayList<>();
        current.add(sorted.getFirst());

        for (int i = 1; i < sorted.size(); i++) {
            Instant previousTs = sorted.get(i - 1).getMetadata().timestamp();
            Instant currentTs = sorted.get(i).getMetadata().timestamp();
            Duration gap = Duration.between(previousTs, currentTs);

            if (gap.compareTo(gapTimeout) > 0) {
                sessions.add(List.copyOf(current));
                current.clear();
            }

            current.add(sorted.get(i));
        }

        if (!current.isEmpty()) {
            sessions.add(List.copyOf(current));
        }

        return sessions;
    }

    @Override
    public String name() {
        return "SessionWindow[gap=" + gapTimeout + "]";
    }

}
