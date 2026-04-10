package com.streametl.window;

import com.streametl.model.DataLakeLayer;
import com.streametl.model.Record;
import com.streametl.model.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WindowAggregatorTest {

    private WindowAggregator aggregator;

    @BeforeEach
    void setUp() {
        aggregator = new WindowAggregator();
    }

    private Record recordAt(Instant timestamp, String field, Object value) {
        RecordMetadata metadata = new RecordMetadata("test", timestamp, DataLakeLayer.BRONZE, 1);
        return new Record(Map.of(field, value), metadata);
    }

    @Test
    void tumblingWindowGroupsRecordsIntoNonOverlappingWindows() {
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        TumblingWindow window = TumblingWindow.of(Duration.ofMinutes(5));

        List<Record> records = List.of(
                recordAt(base, "value", 10),
                recordAt(base.plusSeconds(60), "value", 20),
                recordAt(base.plusSeconds(120), "value", 30),
                recordAt(base.plusSeconds(360), "value", 40),
                recordAt(base.plusSeconds(420), "value", 50)
        );

        List<List<Record>> windows = window.apply(records);

        assertThat(windows).hasSize(2);
        assertThat(windows.get(0)).hasSize(3);
        assertThat(windows.get(1)).hasSize(2);
    }

    @Test
    void tumblingWindowWithNegativeSizeShouldThrow() {
        assertThatThrownBy(() -> TumblingWindow.of(Duration.ofMinutes(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void slidingWindowProducesOverlappingWindows() {
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        SlidingWindow window = SlidingWindow.of(Duration.ofMinutes(10), Duration.ofMinutes(5));

        List<Record> records = List.of(
                recordAt(base, "value", 10),
                recordAt(base.plusSeconds(300), "value", 20),
                recordAt(base.plusSeconds(600), "value", 30)
        );

        List<List<Record>> windows = window.apply(records);

        assertThat(windows).isNotEmpty();
        assertThat(windows.stream().anyMatch(batch -> batch.size() > 1)).isTrue();
    }

    @Test
    void slidingWindowWithSlideExceedingSizeShouldThrow() {
        assertThatThrownBy(() -> SlidingWindow.of(Duration.ofMinutes(5), Duration.ofMinutes(10)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sessionWindowGroupsByGap() {
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        SessionWindow window = SessionWindow.withGap(Duration.ofMinutes(2));

        List<Record> records = List.of(
                recordAt(base, "value", 1),
                recordAt(base.plusSeconds(30), "value", 2),
                recordAt(base.plusSeconds(60), "value", 3),
                recordAt(base.plusSeconds(300), "value", 4),
                recordAt(base.plusSeconds(330), "value", 5)
        );

        List<List<Record>> sessions = window.apply(records);

        assertThat(sessions).hasSize(2);
        assertThat(sessions.get(0)).hasSize(3);
        assertThat(sessions.get(1)).hasSize(2);
    }

    @Test
    void aggregatorComputesSumCorrectly() {
        List<Record> window = List.of(
                Record.of(Map.of("price", new BigDecimal("10.00")), "src"),
                Record.of(Map.of("price", new BigDecimal("20.00")), "src"),
                Record.of(Map.of("price", new BigDecimal("30.00")), "src")
        );

        Optional<BigDecimal> sum = aggregator.sum(window, "price");

        assertThat(sum).isPresent();
        assertThat(sum.get()).isEqualByComparingTo(new BigDecimal("60.00"));
    }

    @Test
    void aggregatorComputesAverageCorrectly() {
        List<Record> window = List.of(
                Record.of(Map.of("price", new BigDecimal("10")), "src"),
                Record.of(Map.of("price", new BigDecimal("20")), "src"),
                Record.of(Map.of("price", new BigDecimal("30")), "src")
        );

        Optional<BigDecimal> avg = aggregator.average(window, "price");

        assertThat(avg).isPresent();
        assertThat(avg.get()).isEqualByComparingTo(new BigDecimal("20"));
    }

    @Test
    void aggregatorComputesMinMaxCorrectly() {
        List<Record> window = List.of(
                Record.of(Map.of("val", new BigDecimal("5")), "src"),
                Record.of(Map.of("val", new BigDecimal("1")), "src"),
                Record.of(Map.of("val", new BigDecimal("9")), "src")
        );

        assertThat(aggregator.min(window, "val")).hasValue(new BigDecimal("1"));
        assertThat(aggregator.max(window, "val")).hasValue(new BigDecimal("9"));
    }

    @Test
    void aggregatorReturnsEmptyOptionalForMissingField() {
        List<Record> window = List.of(
                Record.of(Map.of("other", "x"), "src")
        );

        assertThat(aggregator.sum(window, "missing")).isEmpty();
        assertThat(aggregator.average(window, "missing")).isEmpty();
        assertThat(aggregator.min(window, "missing")).isEmpty();
        assertThat(aggregator.max(window, "missing")).isEmpty();
    }

    @Test
    void statsReturnsAllAggregationsInOneCall() {
        List<Record> window = List.of(
                Record.of(Map.of("amount", new BigDecimal("100")), "src"),
                Record.of(Map.of("amount", new BigDecimal("200")), "src")
        );

        WindowAggregator.AggregateStats stats = aggregator.stats(window, "amount");

        assertThat(stats.count()).isEqualTo(2);
        assertThat(stats.sum()).isEqualByComparingTo(new BigDecimal("300"));
        assertThat(stats.min()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(stats.max()).isEqualByComparingTo(new BigDecimal("200"));
    }

    @Test
    void aggregateWindowsProducesOneStatPerWindow() {
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        TumblingWindow tumbling = TumblingWindow.of(Duration.ofMinutes(5));

        List<Record> records = List.of(
                recordAt(base, "amount", new BigDecimal("10")),
                recordAt(base.plusSeconds(60), "amount", new BigDecimal("20")),
                recordAt(base.plusSeconds(400), "amount", new BigDecimal("30"))
        );

        List<List<Record>> windows = tumbling.apply(records);
        List<WindowAggregator.AggregateStats> stats = aggregator.aggregateWindows(windows, "amount");

        assertThat(stats).hasSize(windows.size());
    }

    @Test
    void emptyWindowReturnsNoSessions() {
        SessionWindow window = SessionWindow.withGap(Duration.ofMinutes(1));
        assertThat(window.apply(List.of())).isEmpty();
    }
}
