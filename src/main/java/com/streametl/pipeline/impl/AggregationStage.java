package com.streametl.pipeline.impl;

import com.streametl.model.Record;
import com.streametl.model.RecordMetadata;
import com.streametl.pipeline.PipelineStage;
import com.streametl.window.WindowAggregator;
import com.streametl.window.WindowFunction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AggregationStage implements PipelineStage {

    private final WindowFunction windowFunction;
    private final String aggregateField;
    private final WindowAggregator aggregator;

    public AggregationStage(WindowFunction windowFunction, String aggregateField) {
        this.windowFunction = windowFunction;
        this.aggregateField = aggregateField;
        this.aggregator = new WindowAggregator();
    }

    public static AggregationStage windowed(WindowFunction window, String field) {
        return new AggregationStage(window, field);
    }

    @Override
    public Record process(Record record) {
        return record;
    }

    public List<Record> processWindow(List<Record> records) {
        List<List<Record>> windows = windowFunction.apply(records);
        List<Record> aggregated = new ArrayList<>();

        for (List<Record> window : windows) {
            if (window.isEmpty()) continue;

            WindowAggregator.AggregateStats stats = aggregator.stats(window, aggregateField);
            Record template = window.getFirst();

            Map<String, Object> summaryFields = new LinkedHashMap<>();
            summaryFields.put("_windowSize", (long) window.size());
            summaryFields.put("_count", stats.count());
            if (stats.sum() != null) summaryFields.put("_sum_" + aggregateField, stats.sum());
            if (stats.average() != null) summaryFields.put("_avg_" + aggregateField, stats.average());
            if (stats.min() != null) summaryFields.put("_min_" + aggregateField, stats.min());
            if (stats.max() != null) summaryFields.put("_max_" + aggregateField, stats.max());

            Record summary = new Record(summaryFields,
                    RecordMetadata.initial(template.getMetadata().sourceId()));
            aggregated.add(summary);
        }

        return aggregated;
    }
}
