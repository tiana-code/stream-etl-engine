package com.streametl.pipeline;

import com.streametl.pipeline.impl.EnrichmentStage;
import com.streametl.pipeline.impl.OutputStage;
import com.streametl.pipeline.impl.TransformationStage;
import com.streametl.pipeline.impl.ValidationStage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public final class PipelineBuilder {

    private final List<PipelineStage> stages = new ArrayList<>();

    private PipelineBuilder() {
    }

    public static PipelineBuilder create() {
        return new PipelineBuilder();
    }

    public PipelineBuilder stage(PipelineStage stage) {
        stages.add(stage);
        return this;
    }

    public PipelineBuilder validate(Set<String> requiredFields) {
        stages.add(ValidationStage.requiring(requiredFields.toArray(String[]::new)));
        return this;
    }

    public PipelineBuilder validate(String... requiredFields) {
        stages.add(ValidationStage.requiring(requiredFields));
        return this;
    }

    public PipelineBuilder validateSoft(String... fields) {
        stages.add(ValidationStage.soft(fields));
        return this;
    }

    public PipelineBuilder enrich(String field, Function<Object, Map<String, Object>> resolver) {
        stages.add(EnrichmentStage.byField(field, resolver));
        return this;
    }

    public PipelineBuilder enrichFromMap(String field, Map<Object, Map<String, Object>> referenceData) {
        stages.add(EnrichmentStage.fromMap(field, referenceData));
        return this;
    }

    public PipelineBuilder transform(TransformationStage transformationStage) {
        stages.add(transformationStage);
        return this;
    }

    public PipelineBuilder output(Consumer<com.streametl.model.Record> sink) {
        stages.add(OutputStage.toSink(sink));
        return this;
    }

    public PipelineBuilder outputNoOp() {
        stages.add(OutputStage.noOp());
        return this;
    }

    public EtlPipeline build() {
        if (stages.isEmpty()) {
            throw new IllegalStateException("Pipeline must have at least one stage");
        }
        return new LinearPipeline(stages);
    }
}
