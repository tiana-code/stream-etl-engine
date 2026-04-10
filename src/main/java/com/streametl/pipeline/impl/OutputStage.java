package com.streametl.pipeline.impl;

import com.streametl.model.Record;
import com.streametl.pipeline.PipelineStage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public final class OutputStage implements PipelineStage {

    private final Consumer<Record> sink;
    private final List<Record> emittedRecords;
    private final boolean captureOutput;

    private OutputStage(Consumer<Record> sink, boolean captureOutput) {
        this.sink = sink;
        this.captureOutput = captureOutput;
        this.emittedRecords = captureOutput ? new ArrayList<>() : List.of();
    }

    public static OutputStage toSink(Consumer<Record> sink) {
        return new OutputStage(sink, false);
    }

    public static OutputStage capturing() {
        return new OutputStage(record -> {
        }, true);
    }

    public static OutputStage noOp() {
        return new OutputStage(record -> {
        }, false);
    }

    @Override
    public Record process(Record record) {
        sink.accept(record);
        if (captureOutput) {
            emittedRecords.add(record);
        }
        return record;
    }

    public List<Record> getEmittedRecords() {
        if (!captureOutput) {
            return List.of();
        }
        return Collections.unmodifiableList(emittedRecords);
    }
}
