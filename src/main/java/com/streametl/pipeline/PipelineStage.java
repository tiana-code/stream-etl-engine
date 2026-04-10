package com.streametl.pipeline;

import com.streametl.model.Record;

@FunctionalInterface
public interface PipelineStage {

    Record process(Record record);

    default PipelineStage andThen(PipelineStage next) {
        return record
                -> next.process(this.process(record));
    }
}
