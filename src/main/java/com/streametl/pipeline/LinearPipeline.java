package com.streametl.pipeline;

import com.streametl.model.Record;

import java.util.List;

final class LinearPipeline implements EtlPipeline {

    private final List<PipelineStage> stages;

    LinearPipeline(List<PipelineStage> stages) {
        this.stages = List.copyOf(stages);
    }

    @Override
    public Record process(Record record) {
        Record current = record;
        for (PipelineStage stage : stages) {
            current = stage.process(current);
        }
        return current;
    }

    @Override
    public List<Record> processAll(List<Record> records) {
        return records.stream()
                .map(this::process)
                .toList();
    }

    @Override
    public int stageCount() {
        return stages.size();
    }

    @Override
    public String describe() {
        return "LinearPipeline[stages=" + stages.size() + "]";
    }
}
