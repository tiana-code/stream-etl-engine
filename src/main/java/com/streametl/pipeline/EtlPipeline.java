package com.streametl.pipeline;

import com.streametl.model.Record;

import java.util.List;

public sealed interface EtlPipeline permits LinearPipeline {

    Record process(Record record);

    List<Record> processAll(List<Record> records);

    int stageCount();

    String describe();
}
