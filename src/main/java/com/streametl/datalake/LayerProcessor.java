package com.streametl.datalake;

import com.streametl.model.DataLakeLayer;
import com.streametl.model.Record;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class LayerProcessor {

    public Record toBronze(Record record) {
        return record.withMetadata(record.getMetadata().withLayer(DataLakeLayer.BRONZE));
    }

    public Record toSilver(Record record) {
        if (record.getMetadata().layer() != DataLakeLayer.BRONZE) {
            throw new IllegalStateException(
                    "Record must be in BRONZE layer before promoting to SILVER, current: "
                            + record.getMetadata().layer());
        }
        Record cleaned = applyCleaningRules(record);
        return cleaned.withMetadata(cleaned.getMetadata().withLayer(DataLakeLayer.SILVER));
    }

    public Record toGold(Record record) {
        if (record.getMetadata().layer() != DataLakeLayer.SILVER) {
            throw new IllegalStateException(
                    "Record must be in SILVER layer before promoting to GOLD, current: "
                            + record.getMetadata().layer()
            );
        }
        return record.withMetadata(record.getMetadata().withLayer(DataLakeLayer.GOLD));
    }

    public List<Record> promoteAll(List<Record> records) {
        return records.stream()
                .map(this::toSilver)
                .map(this::toGold)
                .toList();
    }

    public AggregatedResult aggregate(List<Record> goldRecords, String groupByField) {
        if (goldRecords.isEmpty()) {
            return new AggregatedResult(Map.of(), 0);
        }

        for (Record record : goldRecords) {
            if (record.getMetadata().layer() != DataLakeLayer.GOLD) {
                throw new IllegalStateException(
                        "Expected GOLD layer for aggregation, got: " + record.getMetadata().layer());
            }
        }

        Map<String, List<Record>> grouped = goldRecords.stream()
                .filter(record -> record.has(groupByField))
                .collect(Collectors.groupingBy(record -> Objects.toString(record.get(groupByField), "null")));

        Map<String, Long> counts = grouped.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> (long) entry.getValue().size()));

        int matchedCount = grouped.values().stream().mapToInt(List::size).sum();
        return new AggregatedResult(counts, matchedCount);
    }

    private Record applyCleaningRules(Record record) {
        Record result = record;
        List<String> nullFields = new ArrayList<>();

        for (Map.Entry<String, Object> entry : record.getFields().entrySet()) {
            if (entry.getValue() == null) {
                nullFields.add(entry.getKey());
            } else if (entry.getValue() instanceof String s && s.isBlank()) {
                nullFields.add(entry.getKey());
            }
        }

        for (String field : nullFields) {
            result = result.withoutField(field);
        }

        return result;
    }

    public record AggregatedResult(Map<String, Long> countsByGroup, int totalRecords) {
    }
}
