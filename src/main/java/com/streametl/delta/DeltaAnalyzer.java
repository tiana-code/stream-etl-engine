package com.streametl.delta;

import com.streametl.model.Record;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class DeltaAnalyzer {

    public List<DeltaResult> compare(Record previous, Record current) {
        List<DeltaResult> deltas = new ArrayList<>();
        Map<String, Object> oldFields = previous.getFields();
        Map<String, Object> newFields = current.getFields();

        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(oldFields.keySet());
        allKeys.addAll(newFields.keySet());

        for (String field : allKeys) {
            boolean inOld = oldFields.containsKey(field);
            boolean inNew = newFields.containsKey(field);

            if (!inOld) {
                deltas.add(DeltaResult.added(field, newFields.get(field)));
            } else if (!inNew) {
                deltas.add(DeltaResult.deleted(field, oldFields.get(field)));
            } else if (!Objects.equals(oldFields.get(field), newFields.get(field))) {
                deltas.add(DeltaResult.modified(field, oldFields.get(field), newFields.get(field)));
            }
        }

        return deltas;
    }

    public boolean hasChanges(Record previous, Record current) {
        return !compare(previous, current).isEmpty();
    }

    public List<DeltaResult> compareByChangeType(Record previous, Record current, ChangeType type) {
        return compare(previous, current).stream()
                .filter(delta -> delta.changeType() == type)
                .toList();
    }

    public List<DeltaResult> compareSnapshots(List<Record> previousSnapshot, List<Record> currentSnapshot, String keyField) {
        List<DeltaResult> results = new ArrayList<>();
        Map<Object, Record> previousMap = indexByKey(previousSnapshot, keyField);
        Map<Object, Record> currentMap = indexByKey(currentSnapshot, keyField);

        Set<Object> allKeys = new HashSet<>();
        allKeys.addAll(previousMap.keySet());
        allKeys.addAll(currentMap.keySet());

        for (Object key : allKeys) {
            boolean inPrevious = previousMap.containsKey(key);
            boolean inCurrent = currentMap.containsKey(key);

            if (!inPrevious) {
                results.add(DeltaResult.added(keyField, key));
            } else if (!inCurrent) {
                results.add(DeltaResult.deleted(keyField, key));
            } else {
                results.addAll(compare(previousMap.get(key), currentMap.get(key)));
            }
        }

        return results;
    }

    private Map<Object, Record> indexByKey(List<Record> records, String keyField) {
        Map<Object, Record> index = new LinkedHashMap<>();
        for (Record record : records) {
            Object key = record.get(keyField);
            if (key != null) {
                if (index.containsKey(key)) {
                    throw new IllegalArgumentException(
                            "Duplicate key '%s' in snapshot for field '%s'".formatted(key, keyField));
                }
                index.put(key, record);
            }
        }
        return index;
    }
}
